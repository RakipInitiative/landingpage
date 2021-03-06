<!doctype html>
<html lang="en">
    <head>
        <meta charset="UTF-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />
        <title>RAKIP Data Protection Notice - RAKIP-Web</title>
        <link rel="stylesheet" href="${representation.rakip_endpoint}/assets/css/styles.css" type="text/css">
    </head>
    <body>
        <!-- Assign local backend -->
        <script>
            _endpoint = "${representation.endpoint}";
        </script>
        <!-- vendors js -->
        <script src="${representation.rakip_endpoint}/assets/js/lib/jquery.3.4.1.min.js" ></script>

        <!-- bfr app js -->
        <script src="${representation.rakip_endpoint}/assets/js/dataprotectionnotice.js"></script>
        <!-- page script for initializing app -->
        <script src="${representation.rakip_endpoint}/assets/js/scripts.js"></script>
        <script>
            _appVars.header.brand.logo = "${representation.rakip_endpoint}/assets/img/bfr_logo.gif";
        </script>

        <div class="landingpage pt-0"></div>
        <script>
            _appVars.footer_eu_logo = "${representation.rakip_endpoint}/assets/img/eu_logo.png";
            _appVars.footer_efsa_logo = "${representation.rakip_endpoint}/assets/img/efsa_logo.svg";
        </script>
        <script src="${representation.rakip_endpoint}/assets/js/footer.js"></script>
    </body>
</html>
