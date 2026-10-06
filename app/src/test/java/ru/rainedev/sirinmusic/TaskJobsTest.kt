package ru.rainedev.sirinmusic

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TaskJobsTest {
    @Test fun completedTasksDoNotAccumulate() = runBlocking {
        val jobs = TaskJobs(this)
        repeat(1000) { jobs.launch("favorite:$it") {}.join() }
        assertEquals(0, jobs.size)
    }
    @Test fun replacingATaskCancelsOnlyTheOldTask() = runBlocking {
        val jobs = TaskJobs(this)
        val old = jobs.launch("library") { awaitCancellation() }
        val next = jobs.launch("library") {}
        next.join(); old.join()
        assertTrue(old.isCancelled)
        assertFalse(next.isCancelled)
        assertEquals(0, jobs.size)
    }
    @Test fun cancelAllCanRemoveTasksDuringCompletion() = runBlocking {
        val jobs = TaskJobs(this)
        val tasks = List(20) { jobs.launch("task:$it") { awaitCancellation() } }
        jobs.cancelAll()
        tasks.forEach { it.join(); assertTrue(it.isCancelled) }
        assertEquals(0, jobs.size)
    }
}
