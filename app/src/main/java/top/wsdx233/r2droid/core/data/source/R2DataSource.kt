package top.wsdx233.r2droid.core.data.source

interface R2DataSource {
    suspend fun execute(cmd: String): Result<String>
    suspend fun executeJson(cmd: String): Result<String>
}
