use(function () {
    var o = {}, i, l, name;
    for (i = 0, l = this.names.length; i < l; i += 1) {
        name = this.names[i];
        o[name] = request.getAttribute(name);
    }
    return o;
});
