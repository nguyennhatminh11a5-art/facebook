package com.minifb.demo;

import com.minifb.comment.Comment;
import com.minifb.comment.CommentRepository;
import com.minifb.common.Times;
import com.minifb.friendship.Friendship;
import com.minifb.friendship.FriendshipRepository;
import com.minifb.media.ImageProcessor;
import com.minifb.media.MediaBucket;
import com.minifb.media.MediaFiles;
import com.minifb.media.ProcessedImage;
import com.minifb.post.Post;
import com.minifb.post.PostImage;
import com.minifb.post.PostImageRepository;
import com.minifb.post.PostRepository;
import com.minifb.reaction.ReactionRepository;
import com.minifb.reaction.ReactionType;
import com.minifb.user.User;
import com.minifb.user.UserRepository;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dữ liệu demo (chỉ profile {@code demo}, chỉ chạy khi DB chưa có người dùng). Mật khẩu mọi tài
 * khoản: {@value #PASSWORD}.
 */
@Component
@Profile("demo")
class DemoDataSeeder implements ApplicationRunner {

  static final String PASSWORD = "demo12345";

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

  private final UserRepository users;
  private final FriendshipRepository friendships;
  private final PostRepository posts;
  private final PostImageRepository postImages;
  private final CommentRepository comments;
  private final ReactionRepository reactions;
  private final ImageProcessor imageProcessor;
  private final MediaFiles mediaFiles;
  private final PasswordEncoder passwordEncoder;
  private final TransactionTemplate tx;
  private final Clock clock;

  DemoDataSeeder(
      UserRepository users,
      FriendshipRepository friendships,
      PostRepository posts,
      PostImageRepository postImages,
      CommentRepository comments,
      ReactionRepository reactions,
      ImageProcessor imageProcessor,
      MediaFiles mediaFiles,
      PasswordEncoder passwordEncoder,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.users = users;
    this.friendships = friendships;
    this.posts = posts;
    this.postImages = postImages;
    this.comments = comments;
    this.reactions = reactions;
    this.imageProcessor = imageProcessor;
    this.mediaFiles = mediaFiles;
    this.passwordEncoder = passwordEncoder;
    this.tx = new TransactionTemplate(transactionManager);
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (users.count() > 0) {
      log.info("Demo seed skipped: database already has users");
      return;
    }
    tx.executeWithoutResult(status -> seed());
    log.info("Demo data created. Log in with an@demo.local / {}", PASSWORD);
  }

  private void seed() {
    Instant now = Times.now(clock);
    String hash = passwordEncoder.encode(PASSWORD);

    User an = user("an@demo.local", "An Nguyễn", "Lập trình viên, thích chạy bộ và cà phê.", hash);
    User binh = user("binh@demo.local", "Bình Trần", "Nhiếp ảnh nghiệp dư 📷", hash);
    User chi = user("chi@demo.local", "Chi Lê", "Đà Lạt ❤️", hash);
    User dung = user("dung@demo.local", "Dũng Phạm", null, hash);
    User ha = user("ha@demo.local", "Hà Vũ", "Mê du lịch bụi.", hash);
    User minh = user("minh@demo.local", "Minh Hoàng", "Sinh viên năm cuối.", hash);
    User lan = user("lan@demo.local", "Lan Đỗ", "Yêu mèo và sách.", hash);
    User[] all = {an, binh, chi, dung, ha, minh, lan};
    Color[] palette = {
      new Color(0x2563EB),
      new Color(0x16A34A),
      new Color(0xDB2777),
      new Color(0xEA580C),
      new Color(0x7C3AED),
      new Color(0x0891B2),
      new Color(0xCA8A04)
    };
    for (int i = 0; i < all.length; i++) {
      ProcessedImage avatar =
          imageProcessor.process(avatarImage(all[i].getDisplayName(), palette[i]));
      String key =
          mediaFiles.storeSingle(MediaBucket.AVATARS, avatar.thumbnail(), avatar.extension());
      all[i].changeAvatar(key, now);
    }

    befriend(an, binh, now);
    befriend(an, chi, now);
    befriend(an, dung, now);
    befriend(binh, chi, now);
    befriend(binh, ha, now);
    befriend(chi, minh, now);
    befriend(binh, lan, now);
    befriend(chi, lan, now);
    friendships.save(Friendship.request(ha.getId(), an.getId(), now));
    friendships.save(Friendship.request(an.getId(), minh.getId(), now));

    Post p1 =
        post(
            binh,
            "Hoàng hôn trên đồi chè sáng nay. Ai muốn đi cùng lần sau không?",
            now,
            2,
            landscape(new Color(0xF97316), new Color(0xFDE68A), new Color(0x166534)));
    Post p2 =
        post(an, "Hôm nay deploy xong mini-facebook 🎉 Cảm ơn mọi người đã test giúp!", now, 5);
    Post p3 =
        post(
            chi,
            "Đà Lạt mùa này lạnh quá, nhưng cảnh thì tuyệt vời.",
            now,
            9,
            landscape(new Color(0x60A5FA), new Color(0xE0F2FE), new Color(0x1E3A8A)),
            landscape(new Color(0xA78BFA), new Color(0xFCE7F3), new Color(0x4C1D95)));
    Post p4 = post(dung, "Cuối tuần này có ai đá bóng không? Sân cũ, 7h sáng.", now, 20);
    Post p5 =
        post(
            ha,
            "Chuyến đi Hà Giang 3 ngày 2 đêm — review chi tiết sẽ có trong tuần.",
            now,
            30,
            landscape(new Color(0x10B981), new Color(0xD1FAE5), new Color(0x064E3B)),
            landscape(new Color(0xF59E0B), new Color(0xFEF3C7), new Color(0x78350F)),
            landscape(new Color(0x3B82F6), new Color(0xDBEAFE), new Color(0x1E3A8A)));
    Post p6 = post(an, "Mẹo nhỏ: dùng cursor pagination thay cho OFFSET khi bảng lớn.", now, 48);
    post(minh, "Sắp bảo vệ đồ án rồi, run quá 😅", now, 60);

    comment(p1, an, "Đẹp quá! Lần sau rủ mình với nhé.", now, 1);
    comment(p1, chi, "Chụp bằng máy gì vậy anh?", now, 1);
    comment(p1, binh, "Máy cũ thôi, chủ yếu là may mắn 😄", now, 0);
    comment(p2, dung, "Chúc mừng!", now, 4);
    comment(p2, binh, "Giao diện đẹp đó.", now, 3);
    comment(p3, an, "Nhớ mặc ấm nha.", now, 8);
    comment(p4, an, "Mình tham gia!", now, 19);
    comment(p6, binh, "Hay, cảm ơn nha.", now, 40);

    react(p1, an, ReactionType.LOVE);
    react(p1, chi, ReactionType.WOW);
    react(p1, ha, ReactionType.LIKE);
    react(p2, binh, ReactionType.LIKE);
    react(p2, chi, ReactionType.HAHA);
    react(p2, dung, ReactionType.LOVE);
    react(p3, an, ReactionType.LOVE);
    react(p3, minh, ReactionType.WOW);
    react(p4, an, ReactionType.LIKE);
    react(p5, binh, ReactionType.WOW);
    react(p6, dung, ReactionType.LIKE);
  }

  private User user(String email, String name, String bio, String hash) {
    User user = users.save(new User(email, hash, name, Times.now(clock)));
    user.updateProfile(name, bio, Times.now(clock));
    return user;
  }

  private void befriend(User a, User b, Instant now) {
    Friendship f = Friendship.request(a.getId(), b.getId(), now);
    f.accept(now);
    friendships.save(f);
  }

  private Post post(User author, String content, Instant now, int hoursAgo, byte[]... images) {
    Post post =
        posts.save(new Post(author.getId(), content, now.minus(Duration.ofHours(hoursAgo))));
    List<PostImage> rows = new ArrayList<>();
    for (int i = 0; i < images.length; i++) {
      MediaFiles.StoredImage s =
          mediaFiles.store(MediaBucket.POSTS, imageProcessor.process(images[i]));
      rows.add(new PostImage(post.getId(), s.key(), s.thumbKey(), s.width(), s.height(), i));
    }
    postImages.saveAll(rows);
    return post;
  }

  private void comment(Post post, User author, String content, Instant now, int hoursAgo) {
    comments.save(
        new Comment(post.getId(), author.getId(), content, now.minus(Duration.ofHours(hoursAgo))));
    posts.addCommentCount(post.getId(), 1);
  }

  private void react(Post post, User user, ReactionType type) {
    if (reactions.insertIgnore(post.getId(), user.getId(), type.name(), Times.now(clock)) == 1) {
      posts.addReactionCount(post.getId(), 1);
    }
  }

  private static byte[] avatarImage(String name, Color color) {
    BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = graphics(image);
    g.setPaint(new GradientPaint(0, 0, color.brighter(), 256, 256, color.darker()));
    g.fillRect(0, 0, 256, 256);
    g.setColor(Color.WHITE);
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 110));
    String initial = name.substring(0, 1);
    int width = g.getFontMetrics().stringWidth(initial);
    g.drawString(initial, (256 - width) / 2, 168);
    g.dispose();
    return png(image);
  }

  private static byte[] landscape(Color sky, Color sun, Color hills) {
    int w = 1200;
    int h = 800;
    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = graphics(image);
    g.setPaint(new GradientPaint(0, 0, sky, 0, h, sun));
    g.fillRect(0, 0, w, h);
    g.setColor(new Color(255, 255, 255, 220));
    g.fillOval(w / 2 - 90, h / 2 - 150, 180, 180);
    g.setColor(hills.brighter());
    g.fillPolygon(
        new Polygon(
            new int[] {0, 300, 650, 1000, w, w, 0}, new int[] {560, 380, 520, 360, 470, h, h}, 7));
    g.setColor(hills);
    g.fillPolygon(
        new Polygon(
            new int[] {0, 250, 600, 900, w, w, 0}, new int[] {680, 540, 650, 520, 620, h, h}, 7));
    g.dispose();
    return png(image);
  }

  private static Graphics2D graphics(BufferedImage image) {
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    return g;
  }

  private static byte[] png(BufferedImage image) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
