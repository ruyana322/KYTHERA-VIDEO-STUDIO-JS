package com.example.motion.service

import com.example.motion.model.RenderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

object RenderProgressBus {
  private val _status = MutableStateFlow<RenderStatus>(RenderStatus.Idle)
  val status: StateFlow<RenderStatus> = _status.asStateFlow()

  private val cancelFlag = AtomicBoolean(false)

  fun resetForStart(message: String) {
    cancelFlag.set(false)
    _status.value = RenderStatus.Preparing(message)
  }

  fun update(newStatus: RenderStatus) {
    _status.value = newStatus
  }

  fun requestCancel() {
    cancelFlag.set(true)
  }

  fun isCancelled(): Boolean = cancelFlag.get()

  fun clearFinished() {
    if (_status.value !is RenderStatus.Preparing && _status.value !is RenderStatus.Rendering) {
      _status.value = RenderStatus.Idle
    }
  }
}
