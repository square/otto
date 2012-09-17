Change Log
==========

Version 1.3.1 *(2012-09-17)*
----------------------------

 * Ensure concurrency internally when calling `register`.


Version 1.3.0 *(2012-08-08)*
----------------------------

 * Exceptions in handlers and producers are no longer caught and logged.
 * Producer methods can return `null` to indicate that there is no initial value
   to dispatch. No subscriber methods will be called if `null` is returned.
 * An exception is now thrown if a class attempts to subscribe or produce on
   a non-public method.


Version 1.2.1 *(2012-07-01)*
----------------------------

 * Producer methods can no longer return `null`.


Version 1.2.0 *(2012-06-28)*
----------------------------

 * Add thread enforcer which verifies bus interaction is occuring on a specific
   thread.
 * Add Android sample application demonstrating communication between activity
   and two fragments.
 * Fix: Correct producer unregister failing to remove registration.


Version 1.1.0 *(2012-06-22)*
----------------------------

 * Add `@Produce` method annotation which denotes a method to be invoked for
   an initial value to pass to `@Subscribe` methods when registering. Only a
   single producer may be registered for a type in each event bus instance.


Version 1.0.0 *(2012-06-18)*
----------------------------

Initial release.
