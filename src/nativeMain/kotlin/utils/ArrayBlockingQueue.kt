import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

class ArrayBlockingQueue<E>(private val capacity: Int) {
    private val items = atomicArrayOfNulls<E>(capacity)
    private val head = atomic(0)
    private val tail = atomic(0)
    private val size = atomic(0)
    private val lock = reentrantLock()

    fun offer(element: E): Boolean {
        lock.withLock {
            if (size.value == capacity) {
                return false // Queue is full
            }
            items[tail.value].value = element
            tail.value = (tail.value + 1) % capacity
            size.incrementAndGet()
            return true
        }
    }

    fun poll(): E? {
        lock.withLock {
            if (size.value == 0) {
                return null // Queue is empty
            }
            val element = items[head.value].value
            items[head.value].value = null
            head.value = (head.value + 1) % capacity
            size.decrementAndGet()
            return element
        }
    }

    fun peek(): E? {
        lock.withLock {
            if (size.value == 0) {
                return null // Queue is empty
            }
            return items[head.value].value
        }
    }

    fun isEmpty(): Boolean {
        return size.value == 0
    }

    fun isFull(): Boolean {
        return size.value == capacity
    }
}