# proximity-lock
Exercise demonstrating proximity based android locking

This is an example of one way to lock your android device based on it's proximity
to a location.  Currently a proof of concept, and of course the latest android
release provides similar functionality now natively.  
 
The project is missing a number of useful things:

* Ability to set password (currently hardcoded)
* Ability to use paired bluetooth proximity (in-progress)
* A nicer gui
* ???

If you install this and add some safe locations, it will lock your phone when
you reach the hardcoded distance of 100M.  The password is in the service
class, so look there before installing.

