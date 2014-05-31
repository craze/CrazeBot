<?php

    include("header.inc.php");
    
    $channels = array();
    

// ***********************************************
// **************** Index channels ***************
// ***********************************************
    $path = "logs";

    $ignore = array('.', '..' ); 

    $dh = @opendir( $path ); 
     
    while( false !== ( $file = readdir( $dh ) ) ){ 
     
        if( !in_array( $file, $ignore ) ){ 

            if( is_dir( "$path/$file" ) ){
            
                $dh2 = @opendir( "$path/$file" ); 
                
                unset($logs);
                $logs = array();
                while( false !== ( $file2 = readdir( $dh2 ) ) ){
                
                    if( !in_array( $file2, $ignore ) ){ 
                    
                        $logs[] = $file2;
                        //var_dump($logs);
                        
                    }
                    
                }
                natsort($logs);
                $channels[$file] = $logs;
                
                closedir( $dh2 );
                
            } 
         
        } 
     
    } 
     
    closedir( $dh ); 
    // Close the directory handle 
    
    ksort($channels);

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
        @readfile($log) or die("Log not found.");
?>
    </p>
<?php
    }
    else {
       
     
foreach($channels as $channelName => $logArray){

    if(isset($_GET['channel']) && $channelName != $_GET['channel'])
        continue;
        
    echo "<strong><a href = \"index.php?channel=$channelName\">#{$channelName}</a></strong><br />\n";
    echo "<ul>\n";
    foreach($logArray as $logName){
        $printname = substr($logName,0, strlen($logName) -4);
        echo "<li><a href = \"index.php?log=$path/$channelName/$logName\">$printname</a></li>"; 
    }
    echo "</ul>\n";

}
 

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