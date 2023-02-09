/*
 * build.gradle.kts
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

version = "0.1.0"
group = "com.nikitenkogleb"
description = "Gradle settings convention plugin"

plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
  id("com.gradle.plugin-publish") version "1.1.0"
  signing
}

@Suppress("UnstableApiUsage")
gradlePlugin {
  website.set("https://github.com/Nik-Gleb/nikitenkogleb.com")
  vcsUrl.set("https://github.com/Nik-Gleb/nikitenkogleb.com.git")
  plugins {
    create(project.name) {
      id = "$group.${project.name}"
      displayName = project.name
      description = project.description
      tags.set(listOf("settings", "conventions"))
      implementationClass = "$id.SettingsPlugin"
    }
  }
}

dependencies {
  val version =
    org.gradle.plugin.management.internal.autoapply
      .AutoAppliedGradleEnterprisePlugin.VERSION

  implementation("com.gradle:gradle-enterprise-gradle-plugin:$version")
}
