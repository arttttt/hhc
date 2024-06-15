package utils

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

class ArrayBlockingStack<T>(
    private val capacity: Int,
) {

    private val items = arrayOfNulls<T>(capacity)
    private var size = 0
    private var head = 0
    private val lock = reentrantLock()

    fun add(value: T): Boolean {
        println(
            """
                add before lock
                size = $size
                head = $head
                
            """.trimIndent()
        )

        return lock.withLock {
            if (size == capacity) {
                false
            } else {
                items[head] = value
                head += 1
                size += 1

                true
            }
        }
    }
    fun pop(): T? {
        return lock.withLock {
            if (size == 0) {
                null
            } else {
                head -= 1
                size -= 1
                val result = items[head]
                items[head] = null
                result
            }
        }
    }
}