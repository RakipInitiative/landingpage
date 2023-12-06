<!doctype html>
<html lang="en">
    <head>
        <meta charset="UTF-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />
        <title>KIDA Model Repository</title>
        <link rel="stylesheet" href="${representation.kida_endpoint}/assets/css/styles_kida.css" type="text/css">
    </head>
    <body>
        <!-- Assign local backend -->
        <script>
            _endpoint = "${representation.endpoint}/DB/";
            _token = "${representation.kida_token}";
        </script>
        <!-- vendors js -->
        <script src="${representation.kida_endpoint}/assets/js/lib/jquery.3.4.1.min.js" ></script>
        <!-- bfr data js -->
        <script src="${representation.kida_endpoint}/assets/js/editor_data.js"></script>

        <!-- page script for initializing app -->
        <script src="${representation.kida_endpoint}/assets/js/kida_scripts.js"></script>
        <!-- bfr app js -->
        <script src="${representation.kida_endpoint}/assets/js/app.js"></script>
        <script>
            _appVars.header.brand.logo = "${representation.kida_endpoint}/assets/img/KIDA.png";
        </script>

        <div class="landingpage pt-0"></div>


        <script>
            _appVars.logo_bel = "${representation.rakip_endpoint}/assets/img/bel.svg";
            _appVars.logo_bvl = "${representation.rakip_endpoint}/assets/img/bvl.svg";
            _appVars.logo_fli = "${representation.rakip_endpoint}/assets/img/fli.svg";
            _appVars.logo_dbfz = "${representation.rakip_endpoint}/assets/img/dbfz.svg";
            _appVars.logo_bfr = "${representation.rakip_endpoint}/assets/img/bfr.svg";
            _appVars.logo_jki = "${representation.rakip_endpoint}/assets/img/jki.svg";
            _appVars.logo_mri = "${representation.rakip_endpoint}/assets/img/mri.svg";
            _appVars.logo_thuenen = "${representation.rakip_endpoint}/assets/img/thuenen.svg";
        </script>
        <script src="${representation.rakip_endpoint}/assets/js/footer_kida.js"></script>
    </body>

</html>
