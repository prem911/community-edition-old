<?php

/*
  Copyright (C) 2005 Alfresco, Inc.

  Licensed under the Mozilla Public License version 1.1
  with a permitted attribution clause. You may obtain a
  copy of the License at

    http://www.alfresco.org/legal/license.txt

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
  either express or implied. See the License for the specific
  language governing permissions and limitations under the
  License.
*/
   // Start the session
   session_start();

   require_once('alfresco/RepositoryService.php');
   require_once('alfresco/ContentService.php');
   require_once('alfresco/AuthenticationService.php');
   require_once('alfresco/type/Store.php');
   require_once('alfresco/type/Reference.php');
   require_once('alfresco/tag/TagFramework.php');
   require_once('alfresco/tag/CommonTags.php');


   $authentication_service = new AuthenticationService();
   if ($authentication_service->isUserAuthenticated() == false)
   {
      // Redirect to the login page
      header("Location: /examples/common/login.php?redirect=/examples/browse/index.php");
      exit;
   }

   $auth_details = $authentication_service->getAuthenticationDetails();

   $store = new Store('SpacesStore');
   $reference = null;
   $path = null;

   $repository_service = new RepositoryService($auth_details);
   $content_service = new ContentService($auth_details);

   if (isset($_REQUEST['uuid']) == false)
   {
      $reference = new Reference($store, null, "/app:company_home");
      $path = 'Company Home';

   }
   else
   {
      $reference = new Reference($store, $_REQUEST['uuid']);
      $path = $_REQUEST['path'].'|'.$_REQUEST['uuid'].'|'.$_REQUEST['name'];
   }

   $error_message = "";
   try
   {
      $queryResults = $repository_service->queryChildren($reference);
   }
   catch (Exception $e)
   {
      $error_message = $e->getMessage();
   }
   
   set_exception_handler("exception_handler");
   function exception_handler($exception)
   {
      print "Error: ".$exception->getMessage();
      print "<br>Stack trace: ".$exception->getTraceAsString();
   }

   start_tags();

   function getURL($current_id, $current_name, $path, $current_type="{http://www.alfresco.org/model/content/1.0}folder")
   {
      global $store, $content_service, $auth_details;

      $result = null;
      if ($current_type == "{http://www.alfresco.org/model/content/1.0}content")
      {
         $read_result = $content_service->read(array(new Reference($store, $current_id)), "{http://www.alfresco.org/model/content/1.0}content");
         $result = $read_result->url."?ticket=".$auth_details->getTicket();
      }
      else
      {
         $result = "index.php?".
                     "&uuid=".$current_id.
                     "&name=".$current_name.
                     "&path=".$path;
      }

      return $result;
   }
   
   function getImageURL($current_type="{http://www.alfresco.org/model/content/1.0}folder")
   {
      $result = null;
      if ($current_type == "{http://www.alfresco.org/model/content/1.0}content")
      {
         $result = "post.gif";
      }
      else
      {
         $result = "space_small.gif";
      }

      return $result;
   }

   function outputRow($row)
   {
      global $path;

      $name = $row->getValue('{http://www.alfresco.org/model/content/1.0}name');
      $uuid = $row->uuid();
      $type = $row->type();

      print("<tr><td><img src='/examples/common/images/".getImageURL($type)."'>&nbsp;&nbsp;<a href='");
      print(getURL($uuid, $name, $path, $type));
      print("'>");
      print($name);
      print("</a></td></tr>");
   }
   
   function outputTable($title, $query_results, $type_filter, $empty_message)
   {

     print(
     "<table cellspacing=0 cellpadding=0 border=0 width=95% align=center>".
     "   <tr>".
     "       <td width=7><img src='/examples/common/images/blue_01.gif' width=7 height=7 alt=''></td><td background='/examples/common/images/blue_02.gif'><img src='/examples/common/images/blue_02.gif' width=7 height=7 alt=''></td>".
     "       <td width=7><img src='/examples/common/images/blue_03.gif' width=7 height=7 alt=''></td></tr><tr><td background='/examples/common/images/blue_04.gif'><img src='/examples/common/images/blue_04.gif' width=7 height=7 alt=''></td>".
     "       <td bgcolor='#D3E6FE'>".
     "           <table border='0' cellspacing='0' cellpadding='0' width='100%'><tr><td><span class='mainSubTitle'>".$title."</span></td></tr></table>".
     "       </td>".
     "       <td background='/examples/common/images/blue_06.gif'><img src='/examples/common/images/blue_06.gif' width=7 height=7 alt=''></td>".
     "   </tr>".
     "   <tr>".
     "       <td width=7><img src='/examples/common/images/blue_white_07.gif' width=7 height=7 alt=''></td>".
     "       <td background='/examples/common/images/blue_08.gif'><img src='/examples/common/images/blue_08.gif' width=7 height=7 alt=''></td>".
     "       <td width=7><img src='/examples/common/images/blue_white_09.gif' width=7 height=7 alt=''></td>".
     "   </tr>".
     "   <tr>".
     "       <td background='/examples/common/images/white_04.gif'><img src='/examples/common/images/white_04.gif' width=7 height=7 alt=''></td>".
     "       <td bgcolor='white' style='padding-top:6px;'>".
     "           <table border='0' width='100%'>");

      foreach ($query_results->rows() as $row)
      {
         if ($row->type() == $type_filter)
         {
            outputRow($row);
         }
      }

      print(
      "         </table>".
      "      </td>".
      "      <td background='/examples/common/images/white_06.gif'><img src='/examples/common/images/white_06.gif' width=7 height=7 alt=''></td>".
      "   </tr>".
      "   <tr>".
      "      <td width=7><img src='/examples/common/images/white_07.gif' width=7 height=7 alt=''></td>".
      "      <td background='/examples/common/images/white_08.gif'><img src='/examples/common/images/white_08.gif' width=7 height=7 alt=''></td>".
      "      <td width=7><img src='/examples/common/images/white_09.gif' width=7 height=7 alt=''></td>".
      "   </tr>".
      "</table>");
   }
   
   function outputBreadcrumb($path)
   {
      print(
          '<table border="0" width="95%" align="center">'.
          '   <tr>'.
          '      <td>');

      $values = split("\|", $path);
      $home = $values[0];
      $path = $home;
      $id_map = array();
      for ($counter = 1; $counter < count($values); $counter += 2)
      {
         $id_map[$values[$counter]] = $values[$counter+1];
      }

       print("<a href='index.php'><b>".$home."</b></a>");
       foreach($id_map as $id=>$name)
       {
          $path .= '|'.$id.'|'.$name;
          print("&nbsp;&gt;&nbsp;<a href='".getURL($id, $name, $path)."'><b>".$name."</b></a>");
       }

       print(
        '      </td>'.
        '   </tr>'.
        '</table>');
   }

?>

<html>
   <head>
      <title>Browse Repository</title>
      <style>
         body {font-family: verdana; font-size: 8pt;}
         tr {font-family: verdana; font-size: 8pt;}
         td {font-family: verdana; font-size: 8pt;}
         input {font-family: verdana; font-size: 8pt;}
         .maintitle {font-family: verdana; font-size: 10pt; font-weight: bold; padding-bottom: 15px;}
         a:link, a:visited
         {
      	 font-size: 11px;
      	 color: #465F7D;
      	 text-decoration: none;
      	 font-family: Tahoma, Arial, Helvetica, sans-serif;
      	 font-weight: normal;
        }
        a:hover
        {
        	color: #4272B4;
        	text-decoration: underline;
        	font-weight: normal;
        }
      </style>
   </head>

   <body>

   <table cellspacing=0 cellpadding=2 width=95% align=center>
      <tr>
          <td width=100%>

            <table cellspacing=0 cellpadding=0 width=100%>
            <tr>
               <td style="padding-right:4px;"><img src="/examples/common/images/AlfrescoLogo32.png" border=0 alt="Alfresco" title="Alfresco" align=absmiddle></td>
               <td><img src="/examples/common/images/titlebar_begin.gif" width=10 height=30></td>
               <td width=100% style="background-image: url(/examples/common/images/titlebar_bg.gif)">
                   <b><font style='color: white'>Company Home</font></b>
               </td>
               <td><img src="/examples/common/images/titlebar_end.gif" width=8 height=30></td>
            </tr>
            </table>

          </td>

          <td width=8>&nbsp;</td>
          <td><nobr>
              <img src="/examples/common/images/logout.gif" border=0 alt="Logout (<?php echo $auth_details->getUserName() ?>)" title="Logout (<?php echo $auth_details->getUserName() ?>)" align=absmiddle><span style='padding-left:2px'><a href='/examples/common/login.php?logout=true&redirect=/examples/browse/index.php'>Logout (<?php echo $auth_details->getUserName() ?>)</a></span>
           </nobr></td>
        </tr>
   </table>
   <br>

<?php
   if ($error_message != "")
   {
?>
      <alftag:error error_message='<?php echo $error_message ?>'/>
<?php
   }
   else
   {
       outputBreadcrumb($path);
?>
<br>
<?php
       outputTable("Browse Spaces", $queryResults, "{http://www.alfresco.org/model/content/1.0}folder", "There are no spaces");
?>
<br>
<?php
       outputTable("Content Items", $queryResults, "{http://www.alfresco.org/model/content/1.0}content", "There is no content");
   }
?>

   </body>

</html>
