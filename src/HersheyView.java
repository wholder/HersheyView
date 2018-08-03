import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

class HersheyView extends JPanel {
  private List<HersheyGlyph>  glyphs;
  private int                 index;
  private boolean             showGrid, showLR, showOrigin;
  private double              zoom = 8;

  private HersheyView (List<HersheyGlyph>  glyphs) {
    this.glyphs = glyphs;
  }

  public void paint (Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHints(hints);
    AffineTransform af = new AffineTransform();
    Dimension dim = getSize();
    double xOff = dim.width / 2.0;
    double yOff = dim.height / 2.0;
    af.translate(xOff, yOff);
    af.scale(zoom, zoom);
    if (showGrid) {
      int jj = (int) (50 / (zoom / 8));
      // Draw Grid
      g2.setStroke(new BasicStroke((0.25f)));
      g2.setPaint(Color.blue);
      for (int ii = 0; ii < jj; ii++) {
        g2.draw(af.createTransformedShape(new Line2D.Double(ii, -jj, ii, +jj)));
        g2.draw(af.createTransformedShape(new Line2D.Double(-jj, ii, +jj, ii)));
        if (ii > 0) {
          g2.draw(af.createTransformedShape(new Line2D.Double(-ii, -jj, -ii, +jj)));
          g2.draw(af.createTransformedShape(new Line2D.Double(-jj, -ii, +jj, -ii)));
        }
      }
    }
    if (showOrigin) {
      // Draw cross at origin
      g2.setStroke(new BasicStroke((0.5f)));
      g2.setPaint(Color.red);
      g2.draw(af.createTransformedShape(new Line2D.Double(0, -1, 0, +1)));
      g2.draw(af.createTransformedShape(new Line2D.Double(-1, 0, +1, 0)));
    }
    HersheyGlyph item = glyphs.get(index);
    Rectangle bnds = item.path.getBounds();
    if (showLR) {
      // Draw left/right lines
      g2.setStroke(new BasicStroke((1.0f)));
      g2.setPaint(Color.red);
      g2.draw(af.createTransformedShape(new Line2D.Double(item.left, bnds.y, item.left, bnds.y + bnds.height)));
      g2.draw(af.createTransformedShape(new Line2D.Double(item.right, bnds.y, item.right, bnds.y + bnds.height)));
    }
    g2.setStroke(new BasicStroke((2.0f)));
    g2.setPaint(Color.black);
    g2.draw(af.createTransformedShape(item.path));
    g2.drawString("Code: " + item.code + "  (0x" + Integer.toHexString(item.code).toUpperCase() + ")", 20, 20);
  }

  private void nextGlyph () {
    if (index < glyphs.size() - 1) {
      index++;
      repaint();
    }
  }

  private void prevGlyph () {
    if (index > 0) {
      index--;
      repaint();
    }
  }

  private void setGlyph (int index) {
    this.index = index;
    repaint();
  }

  private void showGrid (boolean enable) {
    showGrid = enable;
    repaint();
  }

  private void showLeftRight (boolean enable) {
    showLR = enable;
    repaint();
  }

  private void showOrigin (boolean enable) {
    showOrigin = enable;
    repaint();
  }

  private void setZoom (String zoom) {
    this.zoom = Double.parseDouble(zoom);
    repaint();
  }

  private Line2D.Double[] getSelectPaths () {
    HersheyGlyph item = glyphs.get(index);
    ArrayList<Line2D.Double> lines = new ArrayList<>();
    PathIterator pi = item.path.getPathIterator(new AffineTransform());
    double xx = 0, yy = 0;
    double mX = 0, mY = 0;
    while (!pi.isDone()) {
      double[] coords = new double[6];
      int type = pi.currentSegment(coords);
      switch (type) {
      case PathIterator.SEG_CLOSE:
        lines.add(new Line2D.Double(xx, yy, mX, mY));
        break;
      case PathIterator.SEG_MOVETO:
        mX = xx = coords[0];
        mY = yy = coords[1];
        break;
      case PathIterator.SEG_LINETO:
        lines.add(new Line2D.Double(xx, yy, xx = coords[0], yy = coords[1]));
        break;
      default:
        System.out.println("Error, Unknown PathIterator Type: " + type);
        break;
      }
      pi.next();
    }
    return lines.toArray(new Line2D.Double[0]);
  }

  static class HersheyGlyph {
    private int           code, left, right;
    private Path2D.Double path;

    HersheyGlyph (int code, Path2D.Double path, int left, int right) {
      this.code = code;
      this.path = path;
      this.left = left;
      this.right = right;
    }
  }

  public static void main (String[] args) {
    String font = "";
    String file = "hershey.txt";
    try {
      InputStream fis = HersheyView.class.getResourceAsStream(file);
      if (fis != null) {
        byte[] data = new byte[fis.available()];
        if (fis.read(data) != data.length) {
          throw new IOException("Unable to read file: " + file);
        }
        fis.close();
        font = new String(data);
      } else {
        throw new IOException("Unable to fine file: " + file);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      showMessageDialog(null, ex.getMessage(), "Error", PLAIN_MESSAGE);
      System.exit(1);
    }
    StringTokenizer tok = new StringTokenizer(font, "\n\r");
    List<String> lines = new ArrayList<>();
    while (tok.hasMoreElements()) {
      String line = tok.nextToken();
      boolean isNewGlyph = true;
      for (int ii = 0; ii < Math.min(4, line.length()); ii++) {
        char cc = line.charAt(ii);
        isNewGlyph &= (Character.isDigit(cc) || cc == ' ');
      }
      if (isNewGlyph) {
        lines.add(line);
      } else {
        int idx = lines.size() - 1;
        String p1 = lines.get(idx) + line;
        lines.remove(idx);
        lines.add(p1);
      }
    }
    List<HersheyGlyph>  glyphs = new ArrayList<>();
    for (String line : lines) {
      Path2D.Double path = new Path2D.Double();
      int code = Integer.parseInt(line.substring(0, 5).trim());
      int verts = Integer.parseInt(line.substring(5, 8).trim());
      int left = line.charAt(8) - 'R';
      int right = line.charAt(9) - 'R';
      boolean move = true;
      for (int ii = 0; ii < verts - 1; ii++) {
        int idx = ii * 2;
        char cx = line.charAt(10 + idx);
        char cy = line.charAt(11 + idx);
        if (cx == ' ' && cy == 'R') {
          move = true;
        } else {
          int xx = cx - 'R';
          int yy = cy - 'R';
          if (move) {
            path.moveTo(xx, yy);
            move = false;
          } else {
            path.lineTo(xx, yy);
          }
        }
      }
      glyphs.add(new HersheyGlyph(code, path, left, right));
    }

    JFrame frame = new JFrame("Hershey Font Viewer");
    frame.setResizable(false);
    frame.setLayout(new BorderLayout());
    HersheyView hershey = new HersheyView(glyphs);
    hershey.setPreferredSize(new Dimension(800, 800));

    frame.add(hershey, BorderLayout.CENTER);
    JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, glyphs.size() - 1, 0);
    slider.addChangeListener(ev -> hershey.setGlyph(slider.getValue()));
    JPanel bottomPane = new JPanel(new BorderLayout());
    bottomPane.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
    bottomPane.add(slider, BorderLayout.CENTER);
    // Define Left button
    JButton undo = new JButton("<");
    undo.addActionListener(e -> hershey.prevGlyph());
    undo.setPreferredSize(new Dimension(24, 12));
    bottomPane.add(undo, BorderLayout.WEST);
    // Define Right button
    JButton redo = new JButton(">");
    redo.addActionListener(e -> hershey.nextGlyph());
    redo.setPreferredSize(new Dimension(24, 12));
    bottomPane.add(redo, BorderLayout.EAST);
    // Add Control panel
    JPanel controls = new JPanel();
    controls.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
    JCheckBox origin = new JCheckBox("Show Origin");
    controls.add(origin);
    origin.addActionListener(ev -> hershey.showOrigin(origin.isSelected()));
    JCheckBox grid = new JCheckBox("Show Grid");
    controls.add(grid);
    grid.addActionListener(ev -> hershey.showGrid(grid.isSelected()));
    JCheckBox leftRight = new JCheckBox("Show L/R");
    controls.add(leftRight);
    leftRight.addActionListener(ev -> hershey.showLeftRight(leftRight.isSelected()));
    JComboBox<String> zoom = new JComboBox<>(new String[] {"8", "16", "32", "64"});
    zoom.addActionListener(ev -> hershey.setZoom((String) zoom.getSelectedItem()));
    controls.add(zoom);
    JButton vectors = new JButton("Line List");
    controls.add(vectors);
    vectors.addActionListener(ev -> {
      JDialog dialog = new JDialog(frame, "Line List", Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.setLocationRelativeTo(hershey);
      JTextArea txt = new JTextArea();
      JScrollPane sPane = new JScrollPane(txt);
      txt.setMargin(new Insets(3, 3, 3, 3));
      txt.setEditable(true);
      txt.setFont(new Font("Courier", Font.PLAIN, 14));
      DecimalFormat df = new DecimalFormat("0");
      for (Line2D.Double line : hershey.getSelectPaths()) {
        txt.append(pad(df.format(line.x1)));
        txt.append(", ");
        txt.append(pad(df.format(line.y1)));
        txt.append(", ");
        txt.append(pad(df.format(line.x2)));
        txt.append(", ");
        txt.append(pad(df.format(line.y2)));
        txt.append("\n");
      }
      txt.setCaretPosition(0);
      dialog.add(sPane, BorderLayout.CENTER);
      dialog.setSize(200, 300);
      Rectangle dim1 = frame.getBounds();
      Rectangle dim2 = vectors.getBounds();
      dialog.setLocation(dim1.x + dim2.x, dim1.y + dim2.y);
      dialog.setVisible(true);
    });
    frame.add(controls, BorderLayout.NORTH);
    frame.add(bottomPane, BorderLayout.SOUTH);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setResizable(false);
    frame.setVisible(true);
  }

  private static String pad (String val) {
    while (val.length() < 3) {
      val = " " + val;
    }
    return val;
  }
}
