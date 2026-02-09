package top.wsdx233.r2droid.core.data.source

import top.wsdx233.r2droid.util.R2PipeManager

class R2PipeDataSource : R2DataSource {
    override suspend fun execute(cmd: String): Result<String> {
        return R2PipeManager.execute(cmd)
    }

    override suspend fun executeJson(cmd: String): Result<String> {
        return R2PipeManager.executeJson(cmd)
    }
}
