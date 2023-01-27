function simplifyBranch() {
    if (true) {
        console.log(true); //sleep 100ms
    }
    console.log(false); //sleep 100ms
}

function simplifyBranch2() {
    console.log(true); //sleep 100ms
    if (true) {
        console.log(false); //sleep 100ms
    }
    console.log(true); //sleep 100ms
}