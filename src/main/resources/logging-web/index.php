<?php

    include("header.inc.php");
    
    function getDirectory( $path = '.', $level = 0 ){ 

    $ignore = array( 'cgi-bin', '.', '..' ); 
    // Directories to ignore when listing output. Many hosts 
    // will deny PHP access to the cgi-bin. 

    $dh = @opendir( $path ); 
    // Open the directory to the handle $dh 
     
    while( false !== ( $file = readdir( $dh ) ) ){ 
    // Loop through the directory 
     
        if( !in_array( $file, $ignore ) ){ 
        // Check that this file is not to be ignored 
             
            $spaces = str_repeat( '&nbsp;', ( $level * 4 ) ); 
            // Just to add spacing to the list, to better 
            // show the directory tree. 
             
            if( is_dir( "$path/$file" ) ){ 
            // Its a directory, so we need to keep reading down... 
                echo "<strong>$spaces #$file</strong><br />"; 
                getDirectory( "$path/$file", ($level+1) );
                echo "<br />\n";
                // Re-call this same function but on a new directory. 
                // this is what makes function recursive. 
             
            } else { 
                $printname = substr($file,0, strlen($file) -4);
                echo "$spaces <a href = \"index.php?log=$path/$file\">$printname</a><br />"; 
                // Just print out the filename 
             
            } 
         
        } 
     
    } 
     
    closedir( $dh ); 
    // Close the directory handle 

} 


    $log = $_GET['log'];
    if (isset($log)) {
?>

    <p>
     <a href="./">Index</a>
    </p>

    <h2>IRC Log <?php echo($log); ?></h2>
    <p>
     Timestamps are in GMT/BST.
    </p>
    <p>
    
<?php
        readfile($log);
?>
    </p>
<?php
    }
    else {
       
     
getDirectory("logs");
 

?>
<?php
        
        
?>
        <a href="<?php echo($_SERVER['PHP_SELF'] . "?date=" . $file); ?>"><?php echo($file); ?></a>
<?php
        }
?>
<?php


    include("footer.inc.php");

?>