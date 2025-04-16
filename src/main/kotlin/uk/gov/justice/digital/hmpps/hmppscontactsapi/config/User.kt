package uk.gov.justice.digital.hmpps.hmppscontactsapi.config

import org.springframework.web.context.request.RequestContextHolder

data class User(val username: String) {
  companion object {
    val SYS_USER = User("SYS")
    const val REQUEST_ATTRIBUTE = "user"

    fun get(): User = RequestContextHolder.getRequestAttributes()
      ?.getAttribute(REQUEST_ATTRIBUTE, 0) as User?
      ?: let {
        val context = SYS_USER
        RequestContextHolder.getRequestAttributes()?.setAttribute(REQUEST_ATTRIBUTE, context, 0)
        context
      }
  }
}
