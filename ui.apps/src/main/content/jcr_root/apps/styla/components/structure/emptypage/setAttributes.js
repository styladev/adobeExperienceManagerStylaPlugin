use(function () {
    var i;
    for (i in this) {
        request.setAttribute(i, this[i]);
    }
});
