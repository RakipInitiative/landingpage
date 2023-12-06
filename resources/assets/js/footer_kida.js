
$(document).ready(function(){
            const footer = document.createElement('footer');
            footer.classList.add('footer');
            footer.style.backgroundColor = 'white';
            const imagesContainer = document.createElement('div'); // Container for images
            imagesContainer.classList.add('images-container');
            const images = [
              { imageUrl: _appVars.logo_bel, link: 'https://www.bmel.de' },
              { imageUrl: _appVars.logo_bvl, link: 'https://www.bvl.bund.de' },
              { imageUrl: _appVars.logo_fli, link: 'https://www.fli.de' },
              { imageUrl: _appVars.logo_dbfz, link: 'https://www.dbfz.de' },
              { imageUrl: _appVars.logo_bfr, link: 'https://www.bfr.bund.de' },
              { imageUrl: _appVars.logo_jki, link: 'https://www.julius-kuehn.de' },
              { imageUrl: _appVars.logo_mri, link: 'https://www.mri.bund.de' },
              { imageUrl: _appVars.logo_thuenen, link: 'https://www.thuenen.de' }
            ];

            images.forEach((imageData) => {
              const imageLink = document.createElement('a');
              imageLink.href = imageData.link;
              imageLink.target = '_blank'; // Open link in a new tab

              const image = document.createElement('img');
              image.src = imageData.imageUrl;
              image.alt = 'KIDA emblem';
              image.style.height = '50px';

              imageLink.appendChild(image);
              imagesContainer.appendChild(imageLink);
            });

            footer.appendChild(imagesContainer);
            const disclaimer = document.createElement('p');
            disclaimer.textContent = 'KIDA ist eine Initiative zur nachhaltigen Stärkung unserer KI- und Datenkompetenzen gemeinsam mit den forschenden und beratenden Einrichtungen des BMEL. Jede Einrichtung hat ein eigenes KIDA Team, das sich zusammensetzt aus Expert(inn)en für KI-Anwendungen, Data Scientis, IT-Fachleuten und dem Projektmanagement. Für KIDA ist vor allem die einrichtungsübergreifende Zusammenarbeit sehr wichtig.';
            footer.appendChild(disclaimer);
            document.body.appendChild(footer);
});
$(function(){
      //Keep track of last scroll
      var lastScroll = 0;
      $(window).scroll(function(event){
          //Sets the current scroll position
          var st = $(this).scrollTop();

          //Determines up-or-down scrolling
          if (st > lastScroll){
            $(".footer").css("display",'inline')
          }
          if(st == 0){
            $(".footer").css("display",'none')
          }
          //Updates scroll position
          lastScroll = st;
      });
    });



