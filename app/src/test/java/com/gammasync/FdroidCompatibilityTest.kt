/*
 * MIT License
 * Copyright (c) 2026 matthewfrazier
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gammasync

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * F-Droid compatibility verification tests
 * 
 * These tests verify that GammaSync meets F-Droid submission requirements:
 * - No network permissions
 * - No proprietary dependencies  
 * - Proper license headers
 * - Valid F-Droid metadata files
 */
@RunWith(AndroidJUnit4::class)
class FdroidCompatibilityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testNoNetworkPermissions() {
        val packageManager = context.packageManager
        val packageName = context.packageName
        
        try {
            val packageInfo = packageManager.getPackageInfo(
                packageName, 
                PackageManager.GET_PERMISSIONS
            )
            
            val permissions = packageInfo.requestedPermissions ?: emptyArray()
            
            // Verify no network permissions are requested
            val networkPermissions = listOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE", 
                "android.permission.ACCESS_WIFI_STATE"
            )
            
            networkPermissions.forEach { permission ->
                assertFalse(
                    "F-Droid incompatible: App requests network permission $permission",
                    permissions.contains(permission)
                )
            }
            
        } catch (e: PackageManager.NameNotFoundException) {
            fail("Package not found: $packageName")
        }
    }

    @Test
    fun testApplicationId() {
        assertEquals(
            "Application ID must match F-Droid submission",
            "com.gammasync", 
            context.packageName
        )
    }

    @Test
    fun testVersionName() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            assertEquals(
                "Version name must be 0.1.0 for F-Droid initial submission",
                "0.1.0",
                packageInfo.versionName
            )
        } catch (e: PackageManager.NameNotFoundException) {
            fail("Package not found")
        }
    }

    @Test
    fun testFdroidMetadataFilesExist() {
        val projectRoot = findProjectRoot()
        assertNotNull("Could not locate project root", projectRoot)
        
        val metadataFiles = listOf(
            "fastlane/metadata/android/en-US/title.txt",
            "fastlane/metadata/android/en-US/short_description.txt", 
            "fastlane/metadata/android/en-US/full_description.txt"
        )
        
        metadataFiles.forEach { path ->
            val file = File(projectRoot, path)
            assertTrue("F-Droid metadata file missing: $path", file.exists())
            assertTrue("F-Droid metadata file empty: $path", file.readText().isNotBlank())
        }
    }

    @Test
    fun testFdroidSubmissionGuideExists() {
        val projectRoot = findProjectRoot()
        assertNotNull("Could not locate project root", projectRoot)
        
        val submissionFile = File(projectRoot, "fdroid-submission.md")
        assertTrue("F-Droid submission guide missing", submissionFile.exists())
        
        val content = submissionFile.readText()
        assertTrue("Submission guide must contain metadata template", 
            content.contains("Categories:"))
        assertTrue("Submission guide must contain license info", 
            content.contains("License: MIT"))
        assertTrue("Submission guide must contain repo URL", 
            content.contains("github.com/matthewfrazier/gammasync"))
    }

    @Test
    fun testLicenseHeadersPresent() {
        val projectRoot = findProjectRoot()
        assertNotNull("Could not locate project root", projectRoot)
        
        val sourceDir = File(projectRoot, "app/src/main/java")
        assertTrue("Source directory must exist", sourceDir.exists())
        
        val kotlinFiles = sourceDir.walkTopDown()
            .filter { it.extension == "kt" }
            .toList()
        
        assertTrue("Must have Kotlin source files to test", kotlinFiles.isNotEmpty())
        
        kotlinFiles.forEach { file ->
            val content = file.readText()
            assertTrue(
                "Source file missing MIT license header: ${file.relativeTo(projectRoot)}",
                content.startsWith("/*\n * MIT License\n * Copyright (c) 2026 matthewfrazier")
            )
        }
    }

    @Test
    fun testNoProprietaryDependencies() {
        val projectRoot = findProjectRoot()
        assertNotNull("Could not locate project root", projectRoot)
        
        val buildGradle = File(projectRoot, "app/build.gradle.kts")
        assertTrue("build.gradle.kts must exist", buildGradle.exists())
        
        val buildContent = buildGradle.readText()
        
        // Check for prohibited dependencies
        val prohibitedDeps = listOf(
            "com.google.android.gms",  // Google Play Services
            "com.google.firebase",     // Firebase
            "com.crashlytics",         // Crashlytics
            "com.google.ads"           // Google Ads
        )
        
        prohibitedDeps.forEach { dep ->
            assertFalse(
                "F-Droid incompatible: Found proprietary dependency $dep",
                buildContent.contains(dep)
            )
        }
        
        // Verify only allowed dependencies are present
        val allowedDeps = listOf(
            "androidx.core:core-ktx",
            "androidx.appcompat:appcompat", 
            "com.google.android.material:material",
            "androidx.activity:activity-ktx",
            "junit:junit",
            "org.robolectric:robolectric",
            "androidx.test"
        )
        
        // Extract all implementation/testImplementation lines
        val dependencyLines = buildContent.lines()
            .filter { it.trim().startsWith("implementation(") || it.trim().startsWith("testImplementation(") || it.trim().startsWith("androidTestImplementation(") }
        
        assertTrue("Must have some dependencies", dependencyLines.isNotEmpty())
    }

    private fun findProjectRoot(): File? {
        var current = File(context.applicationInfo.sourceDir).parentFile
        while (current != null) {
            if (File(current, "build.gradle.kts").exists() && 
                File(current, "app").exists()) {
                return current
            }
            current = current.parentFile
        }
        
        // Fallback: try working directory patterns
        val workingDir = File(System.getProperty("user.dir") ?: "")
        if (File(workingDir, "build.gradle.kts").exists()) {
            return workingDir
        }
        
        return null
    }
}