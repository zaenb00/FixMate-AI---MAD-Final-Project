package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.model.AppNotification
import com.fixmateai.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Exposes the user's live notifications + unread count, and marks them read. */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notifications: LiveData<List<AppNotification>> =
        notificationRepository.notificationsFlow().asLiveData()

    val unreadCount: LiveData<Int> = notifications.map { list -> list.count { !it.read } }

    fun markAllRead() {
        viewModelScope.launch { notificationRepository.markAllRead() }
    }
}
