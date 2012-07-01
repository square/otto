Change Log
==========

Version 1.3.0 *(In Development)*
--------------------------------

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
