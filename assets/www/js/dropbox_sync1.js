    function dropbox_sync_link(){
        console.log("inside the link 1");
        event.preventDefault();
        console.log("inside the link 2");
        dropbox.link();
        console.log("inside the link 3");
        
    }

    function dropbox_sync_unlink(){
              //event.preventDefault();
              app1.showLoader();
              dropbox.unlink().done(function() {
                app1.hideLoader();
                $("#btn-unlink").hide();
		$("#btn-link").show();
		alert("unlinked sucessfully");
                app1.is_dropbox_linked=1;
              }).fail(function(){
                alert("unlinked failed");
              });

    }

    function dropbox_sync_save(){
                 //event.preventDefault();
                 saveImage();
                 var uploadPath=canvas_image_path;
		 app1.showLoader();
                 dropbox.uploadFile(uploadPath).done(function(result) {
                    app1.hideLoader();
                    alert("file uploaded successfully");
                 }).fail(function(err) {
                    console.log('dropbox.uploadFile fail, err -> ' + err);
                    app1.hideLoader();
		    alert("error while uploading");
                 });

    }

    function dropbox_sync_download(){
        //event.preventDefault();
        app1.path = "/";
        app1.showloader();
        app1.listFolder();
        dropbox.addObserver("/");
        
    }
    
    function dropbox_sync_read_file() { // on dropboxView
        //event.preventDefault();
        var filePath = decodeURIComponent($(event.currentTarget).attr("href").substr(1));
        $("#filePath").html(filePath);
        app1.showLoader();
            dropbox.readData(filePath).done(function(result) {
                var bytes = new Uint8Array(result);
                $('#largeImage').attr('src', "data:image/jpeg;base64," + encode(bytes));
                app1.hideLoader();
            });
        display_pic_on_canvas();

    }

    function display_pic_on_canvas(){
    	var myCanvas = document.getElementById('canvas_paint');
	var ctx = myCanvas.getContext('2d');
	var img = document.getElementById('largeImage');
	img.onload = function(){
	  ctx.drawImage(img,0,0,400,400); // Or at whatever offset you like
	};
    }
