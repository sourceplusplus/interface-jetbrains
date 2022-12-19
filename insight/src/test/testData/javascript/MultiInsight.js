function probabilityAndDuration() {
    if (Math.random() > 0.5) { //sleep 100ms
        if (Math.random() > 0.5) { //sleep 100ms
            console.log(false); //sleep 200ms
        }
    } else {
        console.log(true); //sleep 200ms
    }
}