package com.jameel.jvideodownloader.downloads
import kotlinx.coroutines.sync.Semaphore
/** At most one media transfer or publication runs in this application process. */
class DownloadQueue { val permit = Semaphore(1) }
