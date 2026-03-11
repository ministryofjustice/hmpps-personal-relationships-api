package uk.gov.justice.digital.hmpps.personalrelationships.exception

class DuplicateIdentityDocumentException(val type: String, val value: String) : RuntimeException("Contact already has an identity document matching type \"$type\" and value \"$value\"")
