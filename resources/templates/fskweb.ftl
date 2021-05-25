<!doctype html>
<html lang="en">
    <head>
        <meta charset="UTF-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />
        <title>FSK-Web Model Repository</title>
        <link rel="stylesheet" href="${representation.fskweb_endpoint}/assets/css/styles.css" type="text/css">
    </head>
    <body>
        <!-- Assign local backend -->
        <script>
            _endpoint = "${representation.rakip_endpoint}/FSK-Web/";
        </script>
        <!-- vendors js -->
        <script src="${representation.fskweb_endpoint}/assets/js/lib/jquery.3.4.1.min.js" ></script>
        <!-- bfr data js -->
        <script src="${representation.fskweb_endpoint}/assets/js/editor_data.js"></script>
        <!-- bfr app js -->
        <script src="${representation.fskweb_endpoint}/assets/js/fskweb_app.js"></script>
        <!-- page script for initializing app -->
        <script src="${representation.fskweb_endpoint}/assets/js/fskweb_scripts.js"></script>
        <script>
            _appVars.header.brand.logo = "${representation.fskweb_endpoint}/assets/img/bfr_logo.gif";
        </script>

        <div class="landingpage pt-0"></div>
    </body>
</html>
