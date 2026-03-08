package top.wsdx233.r2droid.feature.debug.data

import org.json.JSONArray
import org.json.JSONObject
import top.wsdx233.r2droid.util.R2PipeManager
import javax.inject.Inject

enum class DebugBackend { ESIL, NATIVE_GDB, FRIDA }

class DebuggerRepository @Inject constructor() {

    private fun resolveCommand(
        backend: DebugBackend,
        esilCommand: String,
        nativeCommand: String,
        fridaCommand: String = nativeCommand
    ): Result<String> = when (backend) {
        DebugBackend.ESIL -> Result.success(esilCommand)
        DebugBackend.NATIVE_GDB -> Result.success(nativeCommand)
        DebugBackend.FRIDA -> {
            if (!R2PipeManager.isR2FridaSession) {
                Result.failure(IllegalStateException("FRIDA backend requires an active r2frida session"))
            } else {
                Result.success(":$fridaCommand")
            }
        }
    }

    private suspend fun executeForBackend(
        backend: DebugBackend,
        esilCommand: String,
        nativeCommand: String,
        fridaCommand: String = nativeCommand
    ): Result<String> {
        val command = resolveCommand(backend, esilCommand, nativeCommand, fridaCommand)
            .getOrElse { return Result.failure(it) }
        return R2PipeManager.execute(command)
    }

    private suspend fun executeCandidates(vararg commands: String): Result<String> = runCatching {
        var lastError: Throwable? = null
        for (command in commands.filter { it.isNotBlank() }) {
            val result = R2PipeManager.execute(command)
            if (result.isSuccess) {
                return@runCatching result.getOrThrow()
            }
            lastError = result.exceptionOrNull()
        }
        throw lastError ?: IllegalStateException("No valid command candidates were provided")
    }

    private suspend fun initializeEsil(): Result<String> = runCatching {
        listOf("aei", "aeim", "aeip").forEach { command ->
            R2PipeManager.execute(command).getOrThrow()
        }
        "ESIL initialized"
    }

    private fun parseJsonArray(raw: String): JSONArray {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return JSONArray()

        return try {
            JSONArray(trimmed)
        } catch (_: Exception) {
            val start = trimmed.indexOf('[')
            val end = trimmed.lastIndexOf(']')
            if (start >= 0 && end > start) {
                JSONArray(trimmed.substring(start, end + 1))
            } else {
                throw IllegalArgumentException("Invalid JSON array output")
            }
        }
    }

    private fun parseJsonObject(raw: String): JSONObject {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return JSONObject()

        return try {
            JSONObject(trimmed)
        } catch (_: Exception) {
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            if (start >= 0 && end > start) {
                JSONObject(trimmed.substring(start, end + 1))
            } else {
                throw IllegalArgumentException("Invalid JSON object output")
            }
        }
    }

    private suspend fun readRegisters(vararg commands: String): Result<JSONObject> = runCatching {
        var lastError: Throwable? = null
        for (command in commands.filter { it.isNotBlank() }) {
            val output = R2PipeManager.execute(command).getOrElse {
                lastError = it
                continue
            }

            try {
                return@runCatching parseJsonObject(output)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Unable to read register state")
    }

    private fun parseRegisterValue(value: Any?): Long? = when (value) {
        is Number -> value.toLong()
        is String -> {
            val normalized = value.trim()
            normalized.removePrefix("0x").removePrefix("0X").toLongOrNull(16)
                ?: normalized.toLongOrNull()
        }
        else -> null
    }

    private fun extractProgramCounter(registers: JSONObject): Long? {
        listOf("PC", "pc", "rip", "eip", "ip").forEach { key ->
            parseRegisterValue(registers.opt(key))?.let { return it }
        }

        val keys = registers.keys().asSequence().toList()
        keys.firstOrNull {
            val normalized = it.lowercase()
            normalized == "pc" || normalized == "rip" || normalized == "eip" || normalized == "ip"
        }?.let { key ->
            parseRegisterValue(registers.opt(key))?.let { return it }
        }

        keys.firstOrNull { it.lowercase().endsWith("ip") }?.let { key ->
            parseRegisterValue(registers.opt(key))?.let { return it }
        }

        return null
    }

    private fun parseAddress(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val token = trimmed.lineSequence().firstOrNull()?.trim().orEmpty()
        return token.removePrefix("0x").removePrefix("0X").toLongOrNull(16)
            ?: token.toLongOrNull()
    }

    suspend fun startDebugging(backend: DebugBackend): Result<String> = when (backend) {
        DebugBackend.ESIL -> initializeEsil()
        DebugBackend.NATIVE_GDB -> executeCandidates("ood", "doo")
        DebugBackend.FRIDA -> {
            if (!R2PipeManager.isR2FridaSession) {
                Result.failure(IllegalStateException("FRIDA backend requires an active r2frida session"))
            } else {
                executeCandidates(":drj")
            }
        }
    }

    suspend fun stopDebugging(backend: DebugBackend): Result<String> = when (backend) {
        DebugBackend.ESIL -> executeCandidates("aei-")
        DebugBackend.NATIVE_GDB -> executeCandidates("doc")
        DebugBackend.FRIDA -> Result.success("FRIDA debug controls cleared")
    }

    // 获取当前断点列表
    suspend fun getBreakpoints(): Result<Set<Long>> {
        val command = if (R2PipeManager.isR2FridaSession) ":dbj" else "dbj"
        return R2PipeManager.execute(command).mapCatching {
            val arr = parseJsonArray(it)
        val bps = mutableSetOf<Long>()
        for (i in 0 until arr.length()) {
            bps.add(arr.getJSONObject(i).optLong("addr"))
        }
        bps
        }
    }

    // 切换断点 (只发送指令，状态由ViewModel本地管理)
    suspend fun toggleBreakpoint(addr: Long, isAdd: Boolean): Result<String> {
        val isFrida = R2PipeManager.isR2FridaSession
        val prefix = if (isFrida) ":" else ""
        return if (isAdd) {
            R2PipeManager.execute("${prefix}db $addr")  // 添加
        } else {
            R2PipeManager.execute("${prefix}db- $addr") // 移除
        }
    }

    // 步入 (Step Into) - 适配不同后端
    suspend fun stepInto(backend: DebugBackend): Result<String> =
        executeForBackend(backend, esilCommand = "aes", nativeCommand = "ds")

    // 步过 (Step Over)
    suspend fun stepOver(backend: DebugBackend): Result<String> =
        executeForBackend(backend, esilCommand = "aeso", nativeCommand = "dso")

    // 继续执行 (Continue) - 注意：此命令会阻塞直到遇到断点或崩溃
    suspend fun continueExecution(backend: DebugBackend): Result<String> =
        executeForBackend(backend, esilCommand = "aec", nativeCommand = "dc")

    // 获取当前寄存器状态
    suspend fun getRegisters(backend: DebugBackend): Result<JSONObject> = when (backend) {
        DebugBackend.ESIL -> readRegisters("aerj", "arj", "drj")
        DebugBackend.NATIVE_GDB -> readRegisters("drj", "arj")
        DebugBackend.FRIDA -> {
            if (!R2PipeManager.isR2FridaSession) {
                Result.failure(IllegalStateException("FRIDA backend requires an active r2frida session"))
            } else {
                readRegisters(":drj", ":arj")
            }
        }
    }

    // 获取当前 PC 指针地址 (RIP/PC)
    suspend fun getCurrentPC(backend: DebugBackend): Result<Long> = runCatching {
        val registers = getRegisters(backend).getOrThrow()
        extractProgramCounter(registers)?.let { return@runCatching it }

        val seekCommand = when (backend) {
            DebugBackend.FRIDA -> {
                if (!R2PipeManager.isR2FridaSession) {
                    throw IllegalStateException("FRIDA backend requires an active r2frida session")
                }
                ":s"
            }
            else -> "s"
        }

        parseAddress(R2PipeManager.execute(seekCommand).getOrThrow())
            ?: throw IllegalStateException("Unable to determine program counter")
    }
}
