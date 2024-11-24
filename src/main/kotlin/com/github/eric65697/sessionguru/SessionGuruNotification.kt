package com.github.eric65697.sessionguru

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

private const val NOTIFICATION_GROUP = "Session Guru Notification Group"

fun Project.notifyInfo(content: String) {
  NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP)
    .createNotification(content, NotificationType.INFORMATION).notify(this)
}
fun Project.notifyWarning(content: String) {
  NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP)
    .createNotification(content, NotificationType.WARNING).notify(this)
}
fun Project.notifyErro(content: String) {
  NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP)
    .createNotification(content, NotificationType.ERROR).notify(this)
}
