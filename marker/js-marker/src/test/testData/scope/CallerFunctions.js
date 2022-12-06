function callerFunction() {
    directCalledFunction();
}

function directCalledFunction() {
    indirectCalledFunction();
}

function indirectCalledFunction() {
    console.log(true);
}