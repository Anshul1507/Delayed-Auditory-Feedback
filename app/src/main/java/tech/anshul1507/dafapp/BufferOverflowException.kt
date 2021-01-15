package tech.anshul1507.dafapp

import java.io.IOException

class BufferOverflowException(msg: String?) : IOException(msg) {
    companion object {
        private const val serialVersionUID = -322401823167626048L
    }
}