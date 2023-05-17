using Api.Models;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace Api.DataAccessors;

public sealed class AppDbContext : IdentityDbContext<User>
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    public async Task Initialize(
        UserManager<User> userManager,
        RoleManager<IdentityRole> roleManager,
        IConfiguration configuration)
    {
        if (await Database.EnsureCreatedAsync())
        {
            var admins = configuration.GetSection("Admins").GetChildren();

            foreach (var admin in admins)
            {
                User user = new()
                {
                    SecurityStamp = Guid.NewGuid().ToString(),
                    UserName = (string)admin.GetValue(typeof(string), "UserName"),
                };

                await userManager.CreateAsync(user, (string)admin.GetValue(typeof(string), "Password"));
                await roleManager.CreateAsync(new IdentityRole(Helpers.UserRoles.Admin));
                await userManager.AddToRoleAsync(user, Helpers.UserRoles.Admin);
                await SaveChangesAsync();
            }
        }
    }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<User>().Ignore(x => x.Email);
        modelBuilder.Entity<User>().Ignore(x => x.NormalizedEmail);
        modelBuilder.Entity<User>().Ignore(x => x.EmailConfirmed);
        modelBuilder.Entity<User>().Ignore(x => x.PhoneNumber);
        modelBuilder.Entity<User>().Ignore(x => x.PhoneNumberConfirmed);
        modelBuilder.Entity<User>().Ignore(x => x.TwoFactorEnabled);
        modelBuilder.Entity<User>().Ignore(x => x.LockoutEnd);
        modelBuilder.Entity<User>().Ignore(x => x.LockoutEnabled);
        modelBuilder.Entity<User>().Ignore(x => x.AccessFailedCount);
    }
}
