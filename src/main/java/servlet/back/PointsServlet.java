package servlet.back;

import com.google.gson.Gson;
import dto.PointMarker;
import service.DtoService;
import service.DtoServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/points")
public class PointsServlet extends HttpServlet {
    DtoService dtoService = DtoServiceImpl.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<PointMarker> points = dtoService.getPoints();
        Gson gson = new Gson();
        String json = gson.toJson(points);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().println(json);
    }


}
