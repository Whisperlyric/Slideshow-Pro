package org.teacon.slides.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class UnsignedMessageArgumentType implements ArgumentType<String> {
   public static final SimpleCommandExceptionType INVALID_EXCEPTION = new SimpleCommandExceptionType(Component.literal("Invalid UMA Argument"));

   public static UnsignedMessageArgumentType message() {
      return new UnsignedMessageArgumentType();
   }

   public static String getMessage(CommandContext<CommandSourceStack> context, String name) {
      return (String)context.getArgument(name, String.class);
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      try {
         String string = reader.getString().substring(reader.getCursor(), reader.getTotalLength());
         if (string.isEmpty()) {
            throw INVALID_EXCEPTION.createWithContext(reader);
         } else {
            reader.setCursor(reader.getTotalLength());
            return string;
         }
      } catch (CommandSyntaxException var3) {
         throw var3;
      } catch (Exception var4) {
         throw INVALID_EXCEPTION.create();
      }
   }
}
