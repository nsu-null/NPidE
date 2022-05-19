package ru.nsu_null.npide.ui.console

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import ru.nsu_null.npide.ui.console.Console.AnnotationType.*
import java.io.*

class Console {
    /**
     * Text in console that is currently available
     */
    var content: MutableList<AnnotatedString> = mutableStateListOf()
        private set

    enum class AnnotationType {
        Basic, Error, Special
    }

    /**
     * A simple way to show new text content in console
     * Doesn't get sent to any process
     *
     * Defaults to usual text with no style
     */
    fun display(newContent: String, annotationType: AnnotationType = Basic) {
        if (newContent.isEmpty()) {
            return
        }
        val spanStyle: SpanStyle = when(annotationType) {
            Basic -> SpanStyle()
            Error -> SpanStyle(color = Color.Red, fontWeight = FontWeight(15))
            Special -> SpanStyle(color = Color.Yellow, fontWeight = FontWeight(20))
        }
        val lines = newContent.lines()
        content += lines.map { AnnotatedString(it, spanStyle) }
    }

    /**
     * todo
     */
    fun display(annotatedString: AnnotatedString) {
        content += annotatedString
    }

    /**
     * Delete all content from the console rendering it clear
     */
    fun clear() {
        content.clear()
    }

    /**
     * Write a string to be sent to an attached process if such process exists
     * [display] method is not called, but it could be okay because child process does that anyway
     */
    fun send(command: String) {
        if (!processIsAttached) {
            display(command)
            return
        }
        val process = attachedProcess!!
        val outputStream = process.outputStream
        outputStream.write(command.encodeToByteArray())
        outputStream.flush()
    }

    private var attachedProcess: Process? by mutableStateOf(null)

    /**
     * Label of the attached process, if such process exists, null otherwise
     */
    var attachedProcessLabel: String? = null
        get() = if (!processIsAttached) null else field
        private set

    var processIsAttached: Boolean = false
        get() = attachedProcess != null
        private set

    private var communicationProxyWorker: CommunicationProxyWorker? = null

    /**
     * Attach a process to console for user and ide-wide communication
     * Allows user to write to the process and see the output in the console
     * Allows communicating with the process programmatically using ProcessCommunicationPipes
     * User sees programmatic communication in the console
     *
     * It is caller's responsibility to close given pipes, and caller is expected to read
     * from them so that console is not blocked on writes
     * @param process the process to attach to console
     * @param label some name for this process. The user will see it in the gui
     * @return pipes with which to communicate with the process programmatically
     */
    fun attachProcess(process: Process, label: String): ProcessCommunicationPipes {
        log("Attaching process $label")
        if (processIsAttached) {
            detachCurrentProcess()
        }
        attachedProcess = process
        attachedProcessLabel = label

        val processStdin = PipedOutputStream()
        val processStdout = PipedInputStream()
        val processStderr = PipedInputStream()

        val workerStdin = PipedInputStream().also { it.connect(processStdin) }
        val workerStdout = PipedOutputStream().also { it.connect(processStdout) }
        val workerStderr = PipedOutputStream().also { it.connect(processStderr) }

        communicationProxyWorker = CommunicationProxyWorker(
            this,
            process,
            ConsoleProxyPipes(workerStdin, workerStdout, workerStderr),
            ProcessCommunicationPipes(process.outputStream, process.inputStream, process.errorStream)
        ).also { it.work() }

        return ProcessCommunicationPipes(processStdin, processStdout, processStderr)
    }

    /**
     * Detaches current process from console, if such process exists
     *
     * Calls [Process.destroy] on current process
     */
    fun detachCurrentProcess() {
        val process = attachedProcess
        if (process == null) {
            log("Error: no attached process", Error)
        } else {
            log("Detaching process $attachedProcessLabel")
            communicationProxyWorker?.stop()
            process.destroy()
            communicationProxyWorker = null
            attachedProcess = null
        }
    }

    private fun log(message: String, type: AnnotationType = Special) {
        log("Console", message, type)
    }
}

/**
 * Run a single command or a process
 * Same as [Console.attachProcess], but closes your pipes immediately
 *
 * Note: so far this is not really safe for short commands, because a process can die before
 * all stdin is read, but as we haven't needed that so far, should be ok
 */
fun Console.runProcess(process: Process, label: String) {
    val (stdin, stdout, stderr) = attachProcess(process, label)
    listOf(stdin, stdout, stderr).forEach(Closeable::close)
}

data class ProcessCommunicationPipes(val stdin: OutputStream,
                                     val stdout: InputStream,
                                     val stderr: InputStream)

private data class ConsoleProxyPipes(val stdin: InputStream,
                                     val stdout: OutputStream,
                                     val stderr: OutputStream)

/**
 * [CommunicationProxyWorker] is responsible for proxying client's pipes by writing
 * the messages to console
 *
 * A client is a caller of [Console.attachProcess]
 * @param console console to which to print
 * @param clientPipes streams with which communication with client happens
 * @param processStreams streams with which communication with the process happens
 */
private class CommunicationProxyWorker(private val console: Console,
                                       private val watchedProcess: Process,
                                       clientPipes: ConsoleProxyPipes,
                                       processStreams: ProcessCommunicationPipes) {
    val stdin = processStreams.stdin.writer()
    val stdout = processStreams.stdout.reader()
    val stderr = processStreams.stderr.reader()

    val clientStdin = clientPipes.stdin.reader()
    val clientStdout = clientPipes.stdout.writer()
    val clientStderr = clientPipes.stderr.writer()

    private var thread: Thread? = null
    private var allConnectionsAreFinished = false
    private var drainPipes = false

    fun work() {
        thread = safeThreadContinuousTask(::job) {
            listOf(stdin, stdout, stderr, clientStdin, clientStdout, clientStderr).forEach { stream ->
                try {
                    stream.close()
                } catch (e: IOException) {
                    console.log("ConsoleProxyWorker Warning", "IOException on closing a stream")
                }
            }
        }.also { it.start() }
    }

    private fun job() {
        if (!watchedProcess.isAlive) {
            drainPipes = true
        }
        if (allConnectionsAreFinished) {
            console.detachCurrentProcess()
        }
        communicate()
    }

    private fun communicate() {
        val communicatingPairs = listOf(
            clientStdin to stdin to Basic,
            stdout to clientStdout to Basic,
            stderr to clientStderr to Error
        )
        var closedInputs = 0
        communicatingPairs.forEach { (route, flavour) ->
            val (input, output) = route
            if (communicatePipes(input, output, flavour)) {
                closedInputs++
            }
            val totalInputs = 3
            if (closedInputs == totalInputs) {
                allConnectionsAreFinished = true
            }
        }
    }

    /**
     * @return true iff inputStreamReader was closed during the read
     * Doesn't report outputStreamWriter closing so that console show everything anyway
     */
    private fun communicatePipes(inputStreamReader: InputStreamReader,
                                 outputStreamWriter: OutputStreamWriter,
                                 consoleWriteFlavour: Console.AnnotationType): Boolean {
        val batchSize = 300
        fun readBatch(): CharArray? {
            val acc = CharArray(batchSize)
            if (inputStreamReader.ready() || drainPipes) {
                val readN = inputStreamReader.read(acc)
                if (readN == -1 || readN == 0) {
                    return null
                }
                return acc.sliceArray(0 until readN)
            }
            return CharArray(0)
        }
        val batch = readBatch() ?: return true
        console.display(String(batch), consoleWriteFlavour)
        try {
            outputStreamWriter.write(batch)
        } catch (_: IOException) { /* ignore output is closed */ }
        outputStreamWriter.flush()
        return false
    }

    fun stop() {
        thread?.interrupt()
    }
}

/**
 * Calls [Console.display] and [Console.send]
 *
 * Can be used as a shortcut when the child process doesn't echo written command,
 * but is the desired behaviour
 */
fun Console.displayAndSend(command: String) {
    display(command)
    send(command)
}

/**
 * Log to NPidE console
 * @param who sender name to be displayed
 * @param message text to log
 */
fun Console.log(who: String, message: String, annotationType: Console.AnnotationType = Special) {
    display("[$who]: $message\n", annotationType)
}

private fun safeThreadContinuousTask(task: () -> Unit, onExit: () -> Unit) = Thread {
    while (true) {
        try {
            Thread.sleep(100)
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            task()
        } catch (e: InterruptedException) {
            // usual termination
            onExit()
            return@Thread
        } catch (anyPipeReadWriteException: IOException) {
            // TODO(Roman) investigate whether this is the best way to do that
            continue
        }
    }
}

/**
 * An example of [Console.attachProcess] and [Console.runProcess] usage
 */
private fun main() {

    val console = Console()

    // Create a process
    val gitStatusProcess = Runtime.getRuntime().exec(arrayOf("git", "status"))

    // Immediately attach it. If some sneaky-beaky like action is needed, perform it before attaching
    // But the user won't see any action in the console then
    // Remember to read from needed pipes or close them right after calling!
    val (stdin, stdout, stderr) = console.attachProcess(gitStatusProcess, "git status")

    // Since this moment, only use the given streams to interact with the process so that the users sees everything

    @Suppress("unused")
    fun doStuff() {
        stdin.write("Hello, git!".toByteArray())
        stdout.reader().use { println(it.readText()) }
    }

    // Good manners
    listOf(stdin, stdout, stderr).forEach(Closeable::close)

    Thread.sleep(500) // waiting for the git process to tell everything it needs for presentation purposes

    // You MAY do that in the end. This kills the process
    // If you don't, when the process ends, console will detach itself
    console.detachCurrentProcess()

    println(console.content)

    // if you need not to actually communicate, do this instead
    console.runProcess(gitStatusProcess, "git status i dont care about")
}
