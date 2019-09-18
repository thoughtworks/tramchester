


var app = new Vue({
    el: '#welcome',
    methods: {
        setCookie() {
            var cookie = { 'visited' : true };
            var expiry = moment().add(100, 'days').toDate();
            this.$cookies.set("tramchesterVisited",cookie,"1d");
            window.location.href = 'journeyplan.html';
        }
    },
    mounted () {
        // redir if cookie is set
        var cookie =  this.$cookies.get("tramchesterVisited");
        if (cookie!=null) {
            window.location.href = 'journeyplan.html';
        }
    }
});
