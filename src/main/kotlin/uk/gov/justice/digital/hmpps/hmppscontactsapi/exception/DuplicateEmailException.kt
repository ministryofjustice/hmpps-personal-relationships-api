package uk.gov.justice.digital.hmpps.hmppscontactsapi.exception

class DuplicateEmailException(val email: String) : RuntimeException("Contact already has an email address matching \"$email\"")
