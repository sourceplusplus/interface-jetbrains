function code1() {
    if (false) {
        console.log(true); //sleep 200ms
    }
    console.log(false); //sleep 200ms
}

function code2() {
    if (false) {
        console.log(true); //sleep 200ms
    } else {
        console.log(false); //sleep 200ms
        console.log(false); //sleep 200ms
    }
    console.log(false); //sleep 200ms
}