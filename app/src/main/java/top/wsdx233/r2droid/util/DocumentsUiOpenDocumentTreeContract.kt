package top.wsdx233.r2droid.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

class DocumentsUiOpenDocumentTreeContract : ActivityResultContract<Uri?, Uri?>() {
    private val delegate = ActivityResultContracts.OpenDocumentTree()
    private val preferredPackages = listOf(
        "com.google.android.documentsui",
        "com.android.documentsui"
    )

    override fun createIntent(context: Context, input: Uri?): Intent {
        val intent = delegate.createIntent(context, input)
        val packageManager = context.packageManager
        val matchingPackage = preferredPackages.firstOrNull { packageName ->
            intent.setPackage(packageName)
            intent.resolveActivity(packageManager) != null
        }
        intent.setPackage(matchingPackage)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return delegate.parseResult(resultCode, intent)
    }
}
