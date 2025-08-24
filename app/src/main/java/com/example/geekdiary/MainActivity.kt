package com.example.geekdiary

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import com.example.geekdiary.data.local.PermissionManager
import com.example.geekdiary.navigation.DiaryNavigation
import com.example.geekdiary.ui.theme.GeekDiaryTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Storage permissions are required for asset management",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request storage permissions if needed
        requestStoragePermissionsIfNeeded()

        setContent {
            GeekDiaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DiaryApp()
                }
            }
        }
    }

    private fun requestStoragePermissionsIfNeeded() {
        if (!permissionManager.hasStoragePermissions()) {
            val requiredPermissions = permissionManager.getRequiredStoragePermissions()
            if (requiredPermissions.isNotEmpty()) {
                if (permissionManager.shouldShowStoragePermissionRationale(this)) {
                    // Show rationale dialog
                    Toast.makeText(
                        this,
                        "Storage permissions are needed to save and manage diary assets (images and videos)",
                        Toast.LENGTH_LONG
                    ).show()
                }
                storagePermissionLauncher.launch(requiredPermissions)
            }
        }
    }
}

@Composable
fun DiaryApp() {
    DiaryNavigation()
}

@Preview(showBackground = true)
@Composable
fun DiaryAppPreview() {
    GeekDiaryTheme {
        DiaryApp()
    }
}