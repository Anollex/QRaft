package ro.upt.qraft.utils

/** Continutul codului QR. Folosim ID-ul intern (UUID), unic global, nu codul de inventar. */
fun locationQrContent(id: String) = "qraft://location/$id"
fun itemQrContent(id: String) = "qraft://item/$id"
