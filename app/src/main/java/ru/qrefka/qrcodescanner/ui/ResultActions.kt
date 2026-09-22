package ru.qrefka.qrcodescanner.ui

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.text.format.DateUtils
import android.widget.Toast
import ru.qrefka.qrcodescanner.R
import ru.qrefka.qrcodescanner.util.ScanContent
import java.util.Formatter
import java.util.Locale

/*
 * Every action here hands the payload to another app through an intent that app
 * owns - the dialer, the messaging app, Contacts, Calendar, a map. The user confirms
 * there, so none of it needs a permission of ours (no CALL_PHONE, WRITE_CONTACTS or
 * WRITE_CALENDAR), and the app keeps its camera-only permission set.
 */

internal fun dial(context: Context, number: String) =
    start(context, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))

internal fun sendSms(context: Context, sms: ScanContent.Sms) =
    start(
        context,
        Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", sms.number, null))
            .putExtra("sms_body", sms.body)
    )

internal fun sendEmail(context: Context, email: ScanContent.Email) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email.address, null))
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(email.address))
    if (email.subject.isNotEmpty()) intent.putExtra(Intent.EXTRA_SUBJECT, email.subject)
    if (email.body.isNotEmpty()) intent.putExtra(Intent.EXTRA_TEXT, email.body)
    start(context, intent)
}

/**
 * Opens a map app with a pin on the spot. Devices without one (no geo: handler)
 * get OpenStreetMap in the browser rather than a dead end.
 */
internal fun showOnMap(context: Context, location: ScanContent.Location) {
    val point = "${location.latitude},${location.longitude}"
    val label = location.label.takeIf { it.isNotBlank() }?.let { "(${Uri.encode(it)})" }.orEmpty()
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$point?q=$point$label"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(geo)
    } catch (_: ActivityNotFoundException) {
        val web = "https://www.openstreetmap.org/?mlat=${location.latitude}" +
                "&mlon=${location.longitude}#map=16/${location.latitude}/${location.longitude}"
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(web)))
    }
}

/** Opens the system "new contact" form, prefilled; the user saves it from there. */
internal fun addContact(context: Context, contact: ScanContent.Contact) {
    val intent = Intent(ContactsContract.Intents.Insert.ACTION)
        .setType(ContactsContract.RawContacts.CONTENT_TYPE)
    fun put(key: String, value: String) {
        if (value.isNotBlank()) intent.putExtra(key, value)
    }
    put(ContactsContract.Intents.Insert.NAME, contact.name)
    // The form takes up to three of each through dedicated extras.
    listOf(
        ContactsContract.Intents.Insert.PHONE,
        ContactsContract.Intents.Insert.SECONDARY_PHONE,
        ContactsContract.Intents.Insert.TERTIARY_PHONE
    ).zip(contact.phones).forEach { (key, value) -> put(key, value) }
    listOf(
        ContactsContract.Intents.Insert.EMAIL,
        ContactsContract.Intents.Insert.SECONDARY_EMAIL,
        ContactsContract.Intents.Insert.TERTIARY_EMAIL
    ).zip(contact.emails).forEach { (key, value) -> put(key, value) }
    put(ContactsContract.Intents.Insert.COMPANY, contact.organization)
    put(ContactsContract.Intents.Insert.JOB_TITLE, contact.title)
    put(ContactsContract.Intents.Insert.POSTAL, contact.address)
    put(ContactsContract.Intents.Insert.NOTES, contact.note)
    if (contact.url.isNotBlank()) {
        // A website has no extra of its own; it goes in as a raw data row.
        val website = ContentValues().apply {
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.Website.URL, contact.url)
        }
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, arrayListOf(website))
    }
    start(context, intent)
}

/** Shortcut for a phone-number result: a new contact with just that number. */
internal fun addPhoneContact(context: Context, number: String) =
    start(
        context,
        Intent(ContactsContract.Intents.Insert.ACTION)
            .setType(ContactsContract.RawContacts.CONTENT_TYPE)
            .putExtra(ContactsContract.Intents.Insert.PHONE, number)
    )

/** Opens the calendar's "new event" screen, prefilled; the user saves it from there. */
internal fun addEvent(context: Context, event: ScanContent.Event) {
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
    if (event.title.isNotBlank()) intent.putExtra(CalendarContract.Events.TITLE, event.title)
    event.start?.let { intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
    event.end?.let { intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
    if (event.allDay) intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
    if (event.location.isNotBlank()) intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
    if (event.description.isNotBlank()) intent.putExtra(CalendarContract.Events.DESCRIPTION, event.description)
    start(context, intent)
}

/**
 * Searches for [query] with whatever the device offers for web search. Phones without
 * a search app (common without Google services) get a search page in the browser.
 */
internal fun webSearch(context: Context, query: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_WEB_SEARCH)
                .putExtra(SearchManager.QUERY, query)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bing.com/search?q=" + Uri.encode(query))))
    }
}

/** The event's date range as the user's locale writes it, or null if it has no start. */
internal fun formatEventTime(context: Context, event: ScanContent.Event): String? {
    val start = event.start ?: return null
    var flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or
            DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
    if (!event.allDay) flags = flags or DateUtils.FORMAT_SHOW_TIME
    // All-day times are UTC midnights; formatting them in the local zone would shift
    // the date by one west of Greenwich.
    val zone = if (event.allDay) "UTC" else null
    return DateUtils.formatDateRange(
        context, Formatter(StringBuilder(), Locale.getDefault()), start, event.end ?: start, flags, zone
    ).toString()
}

private fun start(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_app_found), Toast.LENGTH_SHORT).show()
    }
}
