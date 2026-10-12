interface Shape {
    void draw();
}

class Circle implements Shape {
    public void draw() {
        System.out.println("Drawing a Circle");
    }
}

class Rectangle implements Shape {
    public void draw() {
        System.out.println("Drawing a Rectangle");
    }
}

class Square implements Shape {
    public void draw() {
        System.out.println("Drawing a Square");
    }
}

class ShapeFactory {
    public static Shape getShape(String type) {
        if (type == null)
            return null;
        if (type.equalsIgnoreCase("CIRCLE"))
            return new Circle();
        else if (type.equalsIgnoreCase("RECTANGLE"))
            return new Rectangle();
        else if (type.equalsIgnoreCase("SQUARE"))
            return new Square();
        return null;
    }
}

public class Main {
    public static void main(String[] args) {
        Shape s1 = ShapeFactory.getShape("CIRCLE");
        s1.draw();

        Shape s2 = ShapeFactory.getShape("RECTANGLE");
        s2.draw();

        Shape s3 = ShapeFactory.getShape("SQUARE");
        s3.draw();
    }
}