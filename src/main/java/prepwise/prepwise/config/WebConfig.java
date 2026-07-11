package prepwise.prepwise.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        // Serve the file if it exists (JS, CSS, images, etc.)
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // For all other paths (React Router SPA routes), return index.html
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
