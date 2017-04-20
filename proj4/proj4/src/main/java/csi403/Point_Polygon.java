package csi403;


// Import required java libraries
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.json.*;

class Point
{
	//coordinate points
    int x, y;
 
    Point()
    {}
 
	//constructor
    Point(int p, int q)
    {
        x = p;
        y = q;
    }
}

// Extend HttpServlet class
public class Point_Polygon extends HttpServlet {
	
   public static boolean onSegment(Point p, Point q, Point r)
    {
		/*point is known to be colinear with a segment
		**checks if point is on that segment*/
        if (q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x)
                && q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y))
            return true;
        return false;
    }
	
	public static boolean intersectionPoint(Point p1, Point q1, Point p2, Point q2)
	{	
		//line equation 1
		int A1 = q1.y - p1.y;
		int B1 = p1.x - q1.x;
		int C1 = (A1 * p1.x) + (B1 * p1.y);
		
		//line equation 2
		int A2 = q2.y - p2.y;
		int B2 = p2.x - q2.x;
		int C2 = (A2 * p2.x) + (B2 * p2.y);
		
		//finds intersection point
		double det = (A1 * B2) - (A2 * B1);
		
		//assumes intersection exists, so parallel lines (det == 0) will not be a case
		double x = ((B2 * C1) - (B1 * C2)) / det;
		double y = ((A1 * C2) - (A2 * C1)) / det;
		
		//compares intersection point with boundaries
		if((x == p1.x && y == p1.y) || (x == q1.x && y == q1.y))
			return true;
		return false;
	}
		
 
    public static int orientation(Point p, Point q, Point r)
    {
		//find cross product
        int val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
 
        if (val == 0) return 0; //boundary case, vectors are colinear
		
		//if positive, return 1; if negative, return 2
        return (val > 0) ? 1 : 2;
    }
 
    public static boolean doIntersect(Point p1, Point q1, Point p2, Point q2)
    {
        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);
		
		//if orientation == 0, boundary case
		//segments straddle each other: no boundary case
        if (o1 != o2 && o3 != o4)
            return true;	
		
		//segments do not straddle each other: boundary cases apply
        if (o1 == 0 && onSegment(p1, p2, q1))
            return true;
 
        if (o2 == 0 && onSegment(p1, q2, q1))
            return true;
 
        if (o3 == 0 && onSegment(p2, p1, q2))
            return true;
 
        if (o4 == 0 && onSegment(p2, q1, q2))
            return true;
		
		//no intersection
        return false;
    }
 
    public static boolean isInside(Point polygon[], int n, Point p)
    {
        int INF = 10000;
		int EXT = 100000;
        if (n < 3)
			//if number of sides is less than 3: error
            return false;
 
		//a point known to be outside of grid
        Point extreme = new Point(INF, EXT);
 
        int count = 0, i = 0;
        do
        {
            int next = (i + 1) % n;
			
			//if intersection exists
            if (doIntersect(polygon[i], polygon[next], p, extreme))
            {	
				//if boundary case applies
                if (orientation(polygon[i], p, polygon[next]) == 0)
				{
					/*if point is on the boundary, return false
					**boundary case not considered inside polygon*/
					if(onSegment(polygon[i], p, polygon[next]))
						return false;
                    return true;
				}
				
                count++;
            }
            i = next;
        } while (i != 0);
 
		/* if number of intersections is odd, point inside polygon
		** if number of intersections is even, point outside polygon*/
        return (count & 1) == 1 ? true : false;
    }

  // Standard servlet method 
  public void init() throws ServletException
  {
      // Do any required initialization here - likely none
  }

  // Standard servlet method - we will handle a POST operation
  public void doPost(HttpServletRequest request,
                    HttpServletResponse response)
            throws ServletException, IOException
  {
      doService(request, response); 
  }

  // Standard servlet method - we will not respond to GET
  public void doGet(HttpServletRequest request,
                    HttpServletResponse response)
            throws ServletException, IOException
  {
      // Set response content type and return an error message
      response.setContentType("application/json");
      PrintWriter out = response.getWriter();
      out.println("{ 'message' : 'Use POST!'}");
  }

  // Our main worker method
  // Parses messages e.g. {"inList" : [5, 32, 3, 12]}
  // Returns the list sorted.   
  private void doService(HttpServletRequest request,
                    HttpServletResponse response)
            throws ServletException, IOException
  {
	  try{
      // Get received JSON data from HTTP request
      BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
      String jsonStr = "";
      if(br != null){
          jsonStr = br.readLine();
      }
	  
      // Create JsonReader object
      StringReader strReader = new StringReader(jsonStr);
      JsonReader reader = Json.createReader(strReader);

      // Get the singular JSON object (name:value pair) in this message.    
      JsonObject obj = reader.readObject();
      // From the object get the array named "inList"
      JsonArray inArray = obj.getJsonArray("inList");
      JsonArrayBuilder outArrayBuilder = Json.createArrayBuilder();
	  
	  //loop counter & number of points in polygon tracker
	  int i, count = 0;
	  
	  //stores the number of sides in the polygon (number of sides == number of objects in array)
	  int n = inArray.size();
	  
	  String err = "Error: there are less than 3 sides";

	  if(n >= 3) {
		  
		  //stores the coordinate pairs
		  Point polygon[] = new Point[n];
		  
		/*---------------------------------------------------------*/
		  //coordinates stored & polygon created
		  
		  for(i = 0; i < n; i++) {
			//grabs each object in the inList array
			JsonObject coord = inArray.getJsonObject(i);
			polygon[i] = new Point(coord.getInt("x"), coord.getInt("y"));
		  }	  

		/*---------------------------------------------------------*/
		  //19 x 19 grid
		  
		  for(int x = 0; x < 19; x++){
					
			  for(int y = 0; y < 19; y++){
						
				  Point p = new Point(x, y);
						
				  if(isInside(polygon, n, p))
						count++;
			  }
		  }
			
			outArrayBuilder.add(count);
	  }
		
      else {
		  outArrayBuilder.add(err);
	  }		  
      
      // Set response content type to be JSON
      response.setContentType("application/json");
      // Send back the response JSON message
      PrintWriter out = response.getWriter();
      out.println("{ \"count\" : " + outArrayBuilder.build().toString() + "}"); 
	 }
	  
	  //catch errors
	  catch(JsonException e){
		  response.setContentType("application/json");
		  PrintWriter out = response.getWriter();
		  out.println("{ \"message\" : \"Json Exception!\"}");
	  }
	  catch(IllegalStateException e){
		  response.setContentType("application/json");
		  PrintWriter out = response.getWriter();
		  out.println("{ \"message\" : \"illegal state!\"}");
	  }
	  catch(Exception e){
		  response.setContentType("application/json");
		  PrintWriter out = response.getWriter();
		  out.println("{ \"message\" : \"exception!\"}");
	  }
  }	  
	
  // Standard Servlet method
  public void destroy()
  {
      // Do any required tear-down here, likely nothing.
  }
}

