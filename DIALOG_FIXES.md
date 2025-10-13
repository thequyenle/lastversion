# Dialog Fixes - Prevent Closing on Outside Click

## Overview
All dialogs in the app have been updated to **only close when clicking Cancel/No button**, not when clicking outside the dialog or pressing the back button.

## Changes Made

### Method Used
Added `.setCancelable(false)` to all AlertDialog builders and custom Dialog classes.

For custom Dialog classes (extending Dialog), also added:
- `setCancelable(false)`
- `setCanceledOnTouchOutside(false)`

---

## Files Updated

### 1. SetAlarmActivity.kt
**Dialogs Fixed:**
- ✅ Snooze selection dialog
- ✅ Vibration pattern dialog
- ✅ Sound selection dialog

**Changes:**
```kotlin
val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
    .setView(dialogView)
    .setCancelable(false)  // ← Added this
    .create()
```

---

### 2. TimerFragment.kt
**Dialogs Fixed:**
- ✅ Sound picker dialog

**Changes:**
```kotlin
val dialog = AlertDialog.Builder(requireContext())
    .setView(dialogView)
    .setCancelable(false)  // ← Added this
    .create()
```

---

### 3. AlarmFragment.kt
**Dialogs Fixed:**
- ✅ Alarm menu dialog (Duplicate/Delete) - **CAN close by clicking outside** (user request)
- ✅ Delete confirmation dialog - **CANNOT close by clicking outside**

**Changes for Menu Dialog:**
```kotlin
val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
    .setItems(options) { dialog, which -> ... }
    .setCancelable(true)  // ← Allows closing by clicking outside
    .create()

dialog.window?.apply {
    setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
    setLayout(
        WindowManager.LayoutParams.WRAP_CONTENT,  // ← Wrap content width
        WindowManager.LayoutParams.WRAP_CONTENT   // ← Wrap content height
    )
}
```

**Changes for Delete Confirmation Dialog:**
```kotlin
val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
    .setView(dialogView)
    .setCancelable(false)  // ← Cannot close by clicking outside
    .create()
```

---

### 4. AlarmNoteDialog.kt
**Dialog Fixed:**
- ✅ Alarm note input dialog

**Changes:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.dialog_alarm_note)

    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    setCancelable(false)              // ← Added this
    setCanceledOnTouchOutside(false)  // ← Added this
    hideNavigationBar()
    
    initViews()
    setupClickListeners()
}
```

**Also Updated:**
- ✅ Added debounce to Cancel and OK buttons

---

### 5. RatingDialog.kt
**Dialog Fixed:**
- ✅ Rating dialog

**Changes:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.dialog_rating)

    window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    setCancelable(false)              // ← Added this
    setCanceledOnTouchOutside(false)  // ← Added this

    initViews()
    setupInitialState()
    setupListeners()
}
```

**Also Updated:**
- ✅ Added debounce to Vote and Cancel buttons

---

### 6. PermissionHelper.kt
**Dialog Fixed:**
- ✅ Exact alarm permission dialog

**Changes:**
```kotlin
AlertDialog.Builder(fragment.requireContext())
    .setTitle(R.string.exact_alarm_permission_required)
    .setMessage(R.string.exact_alarm_permission_message)
    .setPositiveButton(R.string.open_settings) { _, _ ->
        // ...
    }
    .setNegativeButton(R.string.skip) { _, _ ->
        // ...
    }
    .setCancelable(false)  // ← Added this
    .show()
```

---

### 7. PermissionActivity.kt
**Status:** ✅ Already had `.setCancelable(false)` - No changes needed

---

## Behavior After Fix

### Before:
- ❌ Users could accidentally close dialogs by clicking outside
- ❌ Back button would dismiss dialogs
- ❌ Could lose data/selections

### After:
- ✅ Dialogs only close when clicking Cancel/No/Dismiss button
- ✅ Clicking outside does nothing
- ✅ Back button does nothing
- ✅ Users must make an explicit choice

---

## Testing

To verify the fix works:

1. **Open any dialog** (e.g., Snooze selection, Sound picker, Delete confirmation)
2. **Click outside the dialog** → Dialog should NOT close
3. **Press back button** → Dialog should NOT close
4. **Click Cancel/No button** → Dialog should close
5. **Click OK/Yes button** → Dialog should close and perform action

---

## Additional Improvements

All dialog buttons now also have **1-second debounce** to prevent spam clicking:
- Cancel buttons
- OK buttons
- Yes/No buttons
- Vote buttons

This was implemented using the `setOnClickListenerWithDebounce` extension function.

---

## Notes

- This change improves user experience by preventing accidental dismissals
- Users are forced to make an explicit choice (OK or Cancel)
- Particularly important for destructive actions like "Delete alarm"
- Consistent behavior across all dialogs in the app

