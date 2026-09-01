#!/bin/bash
cat app/src/main/java/com/example/viewmodel/MainViewModel.kt | awk '
/viewModelScope.launch \{/ {
    print $0
    getline
    if (match($0, /repository.checkAndSeedInitialData/)) {
        print "            try {"
        print $0
        getline
        print $0
        getline
        print $0
        getline
        print $0
        getline
        print $0
        print "            } catch(e: Exception) { e.printStackTrace() } finally {"
        print "                validateAndRestoreSession()"
        print "            }"
    } else {
        print $0
    }
    next
}
{ print $0 }
' > app/src/main/java/com/example/viewmodel/MainViewModel.kt.new
mv app/src/main/java/com/example/viewmodel/MainViewModel.kt.new app/src/main/java/com/example/viewmodel/MainViewModel.kt
