/*
 * SettingsPlugin.kt
 *
 * Copyright (c) 2023, Gleb Nikitenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nikitenkogleb.settings

import com.gradle.enterprise.gradleplugin.GradleEnterprisePlugin
import com.gradle.scan.plugin.BuildScanExtension
import groovy.lang.MissingPropertyException
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.resolve.DependencyResolutionManagement
import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.api.initialization.resolve.RulesMode
import org.gradle.api.internal.GradleInternal
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.api.internal.catalog.DefaultVersionCatalogBuilder
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.caching.configuration.BuildCacheConfiguration
import org.gradle.kotlin.dsl.gradleEnterprise
import org.gradle.kotlin.dsl.register
import org.gradle.plugin.management.PluginManagementSpec
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*

@Suppress("unused", "RedundantSuppression")
class SettingsPlugin : Plugin<Settings> {

  override fun apply(target: Settings) {

    target.pluginManagement.apply()
    target.dependencyResolutionManagement.apply()

    target.buildCache.apply(target.settingsDir)

    target.plugins.apply(GradleEnterprisePlugin::class.java)
    target.gradleEnterprise { buildScan { apply() } }

    target.gradle.allprojects {

      if (this.rootProject == this)
        CleanTask.register(tasks)

      if (this.buildFile.exists()) {
        forceCatalogs()
        syncMeta()
      }
    }
  }

  /** Clean Build Cache Task. */
  @Suppress("unused")
  internal abstract class CleanTask : DefaultTask() {

    @TaskAction
    private fun clean() =
      File(this.project.projectDir, Project.DEFAULT_BUILD_DIR_NAME).delete()

    companion object {

      internal fun register(tasks: TaskContainer) =
        tasks.register<CleanTask>("clean")
    }
  }

  private companion object {

    private fun PluginManagementSpec.apply() = repositories.apply()

    private fun BuildScanExtension.apply() {
      termsOfServiceUrl = "https://gradle.com/terms-of-service"
      termsOfServiceAgree = "yes"
      publishAlwaysIf(System.getenv("CI") == "true")
      publishOnFailure()
    }

    @Suppress("UnstableApiUsage")
    private fun DependencyResolutionManagement.apply() {
      repositories.apply()
      repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
      rulesMode.set(RulesMode.PREFER_SETTINGS)
      defaultLibrariesExtensionName.set("libs")
      defaultProjectsExtensionName.set("projects")
      // components.apply()
    }

    private fun RepositoryHandler.apply() {
      val repo = DefaultRepositoryHandler.GRADLE_PLUGIN_PORTAL_REPO_NAME
      val needPlugins = !names.contains(repo)
      if (needPlugins) gradlePluginPortal()
      google()
      mavenCentral()
      mavenLocal()
      //flatDir()
    }

    @Suppress("unused")
    private fun ComponentMetadataHandler.apply() {
      all { }
    }

    private fun BuildCacheConfiguration.apply(value: File) {
      local {
        directory = File(value, Project.DEFAULT_BUILD_DIR_NAME)
        isEnabled = true
        isPush = true
        removeUnusedEntriesAfterDays = 7
      }

      // remote(HttpBuildCache::class.java) {
      // url = uri("http://example.com/cache")
      // isEnabled = false
      // isPush = false
      // isAllowUntrustedServer = false
      // credentials {
      //   username = "john_doe"
      //   password = "secret"
      // }
      // }
    }

    private fun Project.forceCatalogs() {
      val settings = (gradle as GradleInternal).settings
      val catalog = settings.dependencyResolutionManagement.versionCatalogs
      configurations.all {
        resolutionStrategy {
          failOnVersionConflict()
          val strategy = this
          catalog.all {
            val versions = (this as DefaultVersionCatalogBuilder).build()
            versions.libraryAliases.forEach {
              val dependency = versions.getDependencyData(it)
              val notation =
                "${dependency.group}:" +
                  "${dependency.name}:" +
                  "${dependency.version}"
              strategy.force(notation)
            }
          }
        }
      }
    }

    private fun Project.syncMeta() {
      readme()
      changelog()
      groupId()
    }

    private fun Project.readme() {
      description = readme(projectDir, name)
    }

    private fun readme(dir: File, name: String): String {
      val options = StandardOpenOption.WRITE
      val title = "# ${name.cap()}\n\n"
      val file = File(dir, "README.md")
      val path = file.toPath()
      if (!file.exists() && !file.createNewFile()) throw IOException()
      var content = Files.readString(path)
      val replace = !content.startsWith(title)
      content = if (replace) "$title$content" else content
      if (replace) Files.writeString(path, content, options)
      return content.split("\n")[2]
    }

    private fun String.cap() = replaceFirstChar { it.cap() }

    private fun Char.cap() = if (isLowerCase()) titleCase() else toString()

    private fun Char.titleCase() = titlecase(Locale.getDefault())

    private fun Project.changelog() {
      version = changelog(projectDir)
    }

    private fun changelog(dir: File): String {
      val options = StandardOpenOption.WRITE
      val file = File(dir, "CHANGELOG.md")
      val path = file.toPath()
      val exists = file.exists()
      if (!exists && !file.createNewFile()) throw IOException()
      if (!exists) Files.write(
        path, listOf(
          "# Changelog",
          "",
          "## [0.0.0] - 0000-00-00",
          "",
          "---"
        ), options
      )
      return if (!exists) "0.0.0"
      else {
        val content = Files.readString(path)
        content.substring(content.indexOf("[") + 1, content.indexOf("]"))
      }
    }

    private fun Project.groupId() {
      try {
        group =
          "${rootProject.property("groupId")}.${rootProject.name}$path"
            .replace("${Project.PATH_SEPARATOR}$name", "")
            .replace(Project.PATH_SEPARATOR, ".")
      } catch (_: MissingPropertyException) {
      }
    }
  }
}
