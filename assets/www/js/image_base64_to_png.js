/**Works on all versions prior and including Cordova 1.6.1 
* by mcaesar
*  MIT license
*  
*/

(function() {
    /* This increases plugin compatibility */
    var cordovaRef = window.PhoneGap || window.Cordova || window.cordova; // old to new fallbacks

    /**
    * The Java to JavaScript Gateway 'magic' class 
    */
    function Base64ToPNG() { }

    /**
    * Save the base64 String as a PNG file to the user's Photo Library
    */
    function saveImage(b64String, params, win, fail) {
        cordovaRef.exec(win, fail, "Base64ToPNG", "saveImage", [b64String, params]);
    };

    cordovaRef.addConstructor(function() {
        if (!window.plugins) {
            window.plugins = {};
        }
        if (!window.plugins.base64ToPNG) {
            window.plugins.base64ToPNG = new Base64ToPNG();
        }
    });

})(); 
