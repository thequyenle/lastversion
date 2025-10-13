# Button Debounce Implementation

## Overview
This project implements a **1-second debounce** on all clickable buttons to prevent spam clicking. This improves user experience and prevents accidental multiple submissions.

## How It Works

### Extension Function
Located in: `app/src/main/java/net/android/lastversion/utils/ViewExtensions.kt`

```kotlin
fun View.setOnClickListenerWithDebounce(debounceTime: Long = 1000L, action: (View) -> Unit) {
    var lastClickTime = 0L
    
    setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            action(view)
        }
    }
}
```

### Default Debounce Time
- **Default:** 1000ms (1 second)
- **Customizable:** You can pass a different value if needed

## Usage

### Basic Usage (1 second debounce)
Replace:
```kotlin
button.setOnClickListener {
    // your code
}
```

With:
```kotlin
button.setOnClickListenerWithDebounce {
    // your code
}
```

### Custom Debounce Time
```kotlin
button.setOnClickListenerWithDebounce(debounceTime = 500L) {
    // This button has 500ms debounce instead of 1 second
}
```

## Files Updated

### 1. SetAlarmActivity.kt
- ✅ Back button
- ✅ Save button
- ✅ Preview button
- ✅ Snooze layout
- ✅ Vibration layout
- ✅ Sound layout
- ✅ Silent mode switch
- ✅ Alarm note layout
- ✅ All dialog buttons (Cancel, OK)

### 2. TimerFragment.kt
- ✅ Sound picker layout
- ✅ Keep screen switch
- ✅ Start timer button
- ✅ Restart button
- ✅ Stop/Continue button
- ✅ Dialog buttons (Cancel, OK)

### 3. AlarmRingingActivity.kt
- ✅ Dismiss button
- ✅ Snooze button

### 4. AlarmFragment.kt
- ✅ FAB (Add alarm button)
- ✅ Delete confirmation dialog buttons (Yes, No)

## Benefits

1. **Prevents Spam Clicking:** Users can't accidentally click a button multiple times
2. **Better UX:** Prevents duplicate actions (e.g., saving the same alarm twice)
3. **Consistent Behavior:** All buttons across the app have the same debounce behavior
4. **Easy to Maintain:** Single extension function used everywhere
5. **Customizable:** Can adjust debounce time per button if needed

## Adding Debounce to New Buttons

When you create a new button in the future:

1. **Import the extension:**
   ```kotlin
   import net.android.lastversion.utils.setOnClickListenerWithDebounce
   ```

2. **Use it instead of regular setOnClickListener:**
   ```kotlin
   myNewButton.setOnClickListenerWithDebounce {
       // your action
   }
   ```

## Testing

To test the debounce:
1. Try clicking any button rapidly multiple times
2. The action should only execute once per second
3. You won't be able to spam-click buttons anymore

## Notes

- The debounce is **per-button**, not global. Each button tracks its own last click time.
- The debounce time starts from the **last successful click**, not the last attempt.
- This doesn't affect other touch events like scrolling or swiping.

## Future Improvements

If needed, you could:
- Add visual feedback (e.g., disable button temporarily)
- Add haptic feedback on ignored clicks
- Make debounce time configurable from settings
- Add different debounce times for different button types

